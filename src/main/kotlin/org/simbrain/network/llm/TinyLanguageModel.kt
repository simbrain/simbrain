package org.simbrain.network.llm

import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.DeckTile
import org.simbrain.network.compositor.TinyLmCompositor
import org.simbrain.network.compositor.TokenProbabilitySnapshot
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkDebugInfoProvider
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.TensorRole
import org.simbrain.network.tensor.op.*
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.network.trainers.TapeTrainer
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import java.nio.ByteBuffer
import java.util.Base64
import kotlin.collections.ArrayDeque
import kotlin.math.sqrt
import kotlin.random.Random

data class TinyLmConfig(
    val contextSize: Int = 24,
    val embedDim: Int = 20,
    val numHeads: Int = 4,
    val hiddenDim: Int = 30,
    val vocabSize: Int = 100,
    val numLayers: Int = 1,
    val normEps: Float = 1e-5f,
) {
    val headDim get() = embedDim / numHeads

    init {
        require(embedDim % numHeads == 0) { "embedDim $embedDim not divisible by $numHeads heads" }
    }
}

/**
 * Headless GPT-style tiny language model, expressed as one explicit [OpPlan] spanning
 * embed -> (attention -> MLP) x numLayers -> unembed -> sequence cross-entropy. There is no
 * block class: a "layer" is a `layers.<l>.*` range of port names, so [org.simbrain.network.tensor.op.OpPlan.cursor]
 * micro-steps the whole model and one [Tape] records the full loss-to-param path.
 *
 * Unlike [Lfm2Model] this processes the FULL context each forward (no caches) and owns its
 * randomly initialized parameters, trained in place via [Tape] + [TensorAdam]. Pre-norm
 * residuals: the residual trunk is a pure identity path (the visual spine's skip connection);
 * layer norm sits at the entry of each attention/MLP limb.
 */
class TinyLmModel(val config: TinyLmConfig, seed: Long = 42L) {

    private val rng = Random(seed)

    /** Parameters by stable name — the Adam moment keys and the serialization keys. */
    val params = LinkedHashMap<String, TensorPort>()

    private fun param(name: String, rows: Int, cols: Int, init: (Int) -> Float): TensorPort {
        val tensor = FloatTensor(rows, cols, TensorRole.PARAMETER)
        for (i in 0 until tensor.size) tensor.data.put(i, init(i))
        tensor.markMutated()
        return TensorPort(name, tensor).also { params[name] = it }
    }

    private fun weightParam(name: String, rows: Int, cols: Int): TensorPort {
        val bound = 1f / sqrt(cols.toFloat())
        return param(name, rows, cols) { (rng.nextFloat() * 2f - 1f) * bound }
    }

    private fun onesParam(name: String, size: Int) = param(name, 1, size) { 1f }

    private fun zerosParam(name: String, size: Int) = param(name, 1, size) { 0f }

    private fun workspace(name: String, rows: Int, cols: Int) =
        TensorPort(name, FloatTensor(rows, cols).apply { fill(0f) })

    private lateinit var embedOp: SeqEmbedOp
    private lateinit var ceOp: SeqSoftmaxCrossEntropyOp

    val plan: OpPlan = buildPlan()

    val loss: TensorPort get() = plan.port("loss")

    /** Per-position next-token distributions, seq x vocab. */
    val probs: TensorPort get() = plan.port("probs")

    val tape = Tape()
    val grads = Gradients()
    val adam = TensorAdam()

    private fun buildPlan(): OpPlan {
        val c = config
        val seq = c.contextSize
        val dim = c.embedDim
        val ops = ArrayList<TensorOp>()

        val embedTable = weightParam("embed.table", c.vocabSize, dim)
        val posTable = weightParam("embed.pos", seq, dim)
        val embedded = workspace("embed", seq, dim)
        embedOp = SeqEmbedOp("embed", embedTable, embedded)
        ops += embedOp.withDisplayTooltip("Token embeddings", "Look up a learned vector for each token in the context.")
        var resid = workspace("resid0", seq, dim)
        ops += AddOp("add_pos", embedded, posTable, resid)
            .withDisplayTooltip("Add position information", "Combine each token's embedding with its learned position vector.")

        for (l in 0 until c.numLayers) {
            val prefix = "layers.$l"

            val attnNormed = workspace("$prefix.attn.normed", seq, dim)
            ops += LayerNormOp("$prefix.attn.norm", resid,
                onesParam("$prefix.attn.norm.gamma", dim), zerosParam("$prefix.attn.norm.beta", dim),
                attnNormed, c.normEps)
                .withDisplayTooltip("Layer normalization", "Rescale the representation before attention processes it.")
            val q = workspace("$prefix.attn.q", seq, dim)
            val k = workspace("$prefix.attn.k", seq, dim)
            val v = workspace("$prefix.attn.v", seq, dim)
            ops += MatMulLinearOp("$prefix.attn.q_proj", weightParam("$prefix.attn.wq", dim, dim), attnNormed, q)
                .withDisplayTooltip("Query projection", "Create a query vector for each attention head.")
            ops += MatMulLinearOp("$prefix.attn.k_proj", weightParam("$prefix.attn.wk", dim, dim), attnNormed, k)
                .withDisplayTooltip("Key projection", "Create key vectors that queries can compare against.")
            ops += MatMulLinearOp("$prefix.attn.v_proj", weightParam("$prefix.attn.wv", dim, dim), attnNormed, v)
                .withDisplayTooltip("Value projection", "Create the information attention can retrieve from each token.")
            val qHeads = workspace("$prefix.attn.q_heads", c.numHeads * seq, c.headDim)
            val kHeads = workspace("$prefix.attn.k_heads", c.numHeads * seq, c.headDim)
            val vHeads = workspace("$prefix.attn.v_heads", c.numHeads * seq, c.headDim)
            ops += SplitHeadsOp("$prefix.attn.q_split", q, qHeads, c.numHeads)
                .withDisplayTooltip("Split query heads", "Separate the query vectors into independent attention heads.")
            ops += SplitHeadsOp("$prefix.attn.k_split", k, kHeads, c.numHeads)
                .withDisplayTooltip("Split key heads", "Separate the key vectors into independent attention heads.")
            ops += SplitHeadsOp("$prefix.attn.v_split", v, vHeads, c.numHeads)
                .withDisplayTooltip("Split value heads", "Separate the value vectors into independent attention heads.")
            val scores = workspace("$prefix.attn.scores", c.numHeads * seq, seq)
            ops += HeadScoresOp("$prefix.attn.score", qHeads, kHeads, scores, c.numHeads)
                .withDisplayTooltip("Attention scores", "Compare each query with earlier keys to measure their relevance.")
            val attnWeights = workspace("$prefix.attn.weights", c.numHeads * seq, seq)
            ops += CausalMaskedRowSoftmaxOp("$prefix.attn.softmax", scores, attnWeights, c.numHeads)
                .withDisplayTooltip("Attention weights", "Convert scores into weights; tokens cannot attend to future tokens.")
            val mixed = workspace("$prefix.attn.mixed", c.numHeads * seq, c.headDim)
            ops += HeadMixOp("$prefix.attn.mix", attnWeights, vHeads, mixed, c.numHeads)
                .withDisplayTooltip("Mix values", "Use the attention weights to combine information from tokens in the context.")
            val merged = workspace("$prefix.attn.merged", seq, dim)
            ops += MergeHeadsOp("$prefix.attn.merge", mixed, merged, c.numHeads)
                .withDisplayTooltip("Merge attention heads", "Join the independent head outputs into one representation.")
            val attnOut = workspace("$prefix.attn.out", seq, dim)
            ops += MatMulLinearOp("$prefix.attn.out_proj", weightParam("$prefix.attn.wo", dim, dim), merged, attnOut)
                .withDisplayTooltip("Attention output projection", "Return the combined attention information to the model's main width.")
            val attnResid = workspace("$prefix.attn_resid", seq, dim)
            ops += AddOp("$prefix.attn_residual", resid, attnOut, attnResid)
                .withDisplayTooltip("Residual addition", "Add the attention update to the running representation.")

            val mlpNormed = workspace("$prefix.mlp.normed", seq, dim)
            ops += LayerNormOp("$prefix.mlp.norm", attnResid,
                onesParam("$prefix.mlp.norm.gamma", dim), zerosParam("$prefix.mlp.norm.beta", dim),
                mlpNormed, c.normEps)
                .withDisplayTooltip("Layer normalization", "Rescale the representation before the MLP processes it.")
            val hiddenRaw = workspace("$prefix.mlp.hidden_raw", seq, c.hiddenDim)
            val hidden = workspace("$prefix.mlp.hidden", seq, c.hiddenDim)
            val act = workspace("$prefix.mlp.act", seq, c.hiddenDim)
            val outRaw = workspace("$prefix.mlp.out_raw", seq, dim)
            val mlpOut = workspace("$prefix.mlp.out", seq, dim)
            ops += MatMulLinearOp("$prefix.mlp.up_proj", weightParam("$prefix.mlp.w1", c.hiddenDim, dim), mlpNormed, hiddenRaw)
                .withDisplayTooltip("MLP expansion", "Expand the representation into a larger set of features.")
            ops += BiasOp("$prefix.mlp.up_bias", hiddenRaw, zerosParam("$prefix.mlp.b1", c.hiddenDim), hidden)
                .withDisplayTooltip("Add bias", "Give each MLP feature its own learned offset.")
            ops += ReLUOp("$prefix.mlp.relu", hidden, act)
                .withDisplayTooltip("ReLU activation", "Keep positive features and set negative features to zero.")
            ops += MatMulLinearOp("$prefix.mlp.down_proj", weightParam("$prefix.mlp.w2", dim, c.hiddenDim), act, outRaw)
                .withDisplayTooltip("MLP compression", "Project the selected features back to the model's main width.")
            ops += BiasOp("$prefix.mlp.down_bias", outRaw, zerosParam("$prefix.mlp.b2", dim), mlpOut)
                .withDisplayTooltip("Add bias", "Give each output feature its own learned offset.")
            val layerResid = workspace("$prefix.resid", seq, dim)
            ops += AddOp("$prefix.residual", attnResid, mlpOut, layerResid)
                .withDisplayTooltip("Residual addition", "Add the MLP update to the running representation.")
            resid = layerResid
        }

        val finalNormed = workspace("final_normed", seq, dim)
        ops += LayerNormOp("final_norm", resid,
            onesParam("final_norm.gamma", dim), zerosParam("final_norm.beta", dim),
            finalNormed, c.normEps)
            .withDisplayTooltip("Output normalization", "Rescale the final representation before scoring next-token choices.")
        val logits = workspace("logits", seq, c.vocabSize)
        ops += MatMulLinearOp("unembed", weightParam("unembed.weight", c.vocabSize, dim), finalNormed, logits)
            .withDisplayTooltip("Next-token scores", "Score every possible next token from the final representation.")
        ceOp = SeqSoftmaxCrossEntropyOp("cross_entropy", logits,
            workspace("probs", seq, c.vocabSize), workspace("loss", 1, 1))
        ops += ceOp.withDisplayTooltip("Training loss", "Measure how far the predicted probabilities are from the expected next tokens.")

        return OpPlan(ops)
    }

    /**
     * Binds one sample: [tokens] fill the context from position 0 (shorter contexts pad with -1,
     * which embeds to zero rows), [targets] supervise matching positions (-1 = unsupervised).
     */
    fun setSample(tokens: IntArray, targets: IntArray = IntArray(0)) {
        require(tokens.size <= config.contextSize) { "${tokens.size} tokens > context ${config.contextSize}" }
        require(targets.isEmpty() || targets.size == tokens.size) {
            "targets size ${targets.size} != tokens size ${tokens.size}"
        }
        embedOp.tokenIds = IntArray(config.contextSize) { tokens.getOrElse(it) { -1 } }
        ceOp.targetIds = IntArray(config.contextSize) { targets.getOrElse(it) { -1 } }
    }

    /** Full forward pass on the bound sample; returns the loss (0 when nothing is supervised). */
    @Synchronized
    fun forward(): Float {
        plan.forward()
        return loss.tensor.data.get(0)
    }

    /** True once a backward pass has written gradients; false until then and after a rebuild. */
    var gradientsComputed = false
        private set

    /** One tape-recorded forward + backward + Adam update on [tokens]/[targets]; returns the loss. */
    @Synchronized
    fun trainStep(tokens: IntArray, targets: IntArray): Float {
        setSample(tokens, targets)
        tape.clear()
        grads.zeroAll()
        plan.forward(tape)
        val lossValue = loss.tensor.data.get(0)
        tape.backward(loss, grads)
        gradientsComputed = true
        applyOptimizer()
        return lossValue
    }

    private fun applyOptimizer() {
        adam.step()
        params.forEach { (name, port) -> adam.update(name, port.tensor, grads.of(port.tensor)) }
    }

    enum class StepPhase { IDLE, FORWARD, BACKWARD }

    /** Where a micro-stepped training step currently is; [stepOp] advances through the phases. */
    var stepPhase = StepPhase.IDLE
        private set

    /** True while a stepped walk or a partial forward pass holds the plan mid-flight. */
    val midWalk: Boolean
        get() = stepPhase != StepPhase.IDLE || plan.cursor != 0

    /**
     * Arms a micro-stepped training step on [tokens]/[targets]: subsequent [stepOp] calls run one
     * op at a time — the whole forward pass, then every VJP in reverse, then the Adam update —
     * with [stepPhase] tracking where the walk is.
     */
    @Synchronized
    fun beginSteppedTrainStep(tokens: IntArray, targets: IntArray) {
        check(stepPhase == StepPhase.IDLE) { "A stepped train step is already in progress" }
        check(plan.cursor == 0) { "Plan is mid-pass at op ${plan.cursor}" }
        setSample(tokens, targets)
        tape.clear()
        grads.zeroAll()
        stepPhase = StepPhase.FORWARD
    }

    /** The op the next [stepOp] will run, or null when idle. */
    fun nextOp(): TensorOp? = when (stepPhase) {
        StepPhase.IDLE -> null
        StepPhase.FORWARD -> plan.ops[plan.cursor]
        StepPhase.BACKWARD -> tape.peekBackward()
    }

    /**
     * Advances a stepped training step by one op. Forward completion arms the backward pass;
     * backward completion applies the optimizer and returns to idle.
     */
    @Synchronized
    fun stepOp(): TensorOp {
        return when (stepPhase) {
            StepPhase.IDLE -> error("No stepped train step in progress; call beginSteppedTrainStep")
            StepPhase.FORWARD -> plan.stepOp(tape).also {
                if (plan.cursor == 0) {
                    tape.beginBackward(loss, grads)
                    gradientsComputed = true
                    stepPhase = StepPhase.BACKWARD
                }
            }
            StepPhase.BACKWARD -> tape.stepBackward(grads).also {
                if (!tape.isBackwardInProgress) {
                    applyOptimizer()
                    stepPhase = StepPhase.IDLE
                }
            }
        }
    }

    /**
     * Advances a plain inference pass by one op (no tape, no gradients) — token/layer/op-level
     * stepping outside training. Only valid when no stepped training step is active.
     */
    @Synchronized
    fun stepForwardOnly(): TensorOp {
        check(stepPhase == StepPhase.IDLE) { "A stepped train step is in progress" }
        return plan.stepOp()
    }

    /** Next-token distribution at [position] (the row of [probs] the next token is sampled from). */
    fun distributionAt(position: Int): FloatArray {
        val p = probs.tensor
        return FloatArray(p.cols) { p[position, it] }
    }
}

class TinyLanguageModelEvents : LocationEvents() {
    val modelRebuilt = NoArgEvent()

    /** A step request declined, with why — the GUI surfaces the reason as a transient notice. */
    val stepRefused = OneArgEvent<TinyLanguageModel.StepRefusal>()
}

/**
 * The tiny language model on the network canvas: wraps the headless [TinyLmModel],
 * its [TapeTrainer], and the compositor spine scene. Each network update runs a full forward pass
 * on the current context, so the workspace drives generation while the interior shows the whole
 * computation.
 *
 * Serialization: the config, corpus, view state, AND the trained weights go into the workspace
 * file — this is a trained-in-place teaching artifact, unlike [LanguageModel], which reloads its
 * weights from disk. Weight capture happens at marshal time through the companion's property
 * converter, so whatever training did up to the save is what comes back.
 *
 * The coupling vocabulary and document-sync protocol live on [GenerativeModel], here over a
 * word-level vocabulary: text maps through [tokenizer] and [tokenLabels], and words outside
 * the vocabulary are dropped.
 */
class TinyLanguageModel @XStreamConstructor constructor() : GenerativeModel(), NetworkDebugInfoProvider {

    override val displayTokenizer: Tokenizer<*>
        get() = tokenizer

    var config: TinyLmConfig = TinyLmConfig()
        private set

    var model: TinyLmModel = TinyLmModel(config)
        private set

    /** Vocabulary index to token string, for lens readouts and status text. */
    var tokenLabels: ArrayList<String>? = null

    var corpusTokenIds: IntArray? = null
        private set

    var testCorpusTokenIds: IntArray? = null
        private set

    /** The context window the next forward pass reads, most recent tokens last. */
    var contextTokens: IntArray = IntArray(0)
        private set

    var learningRate by GuiEditable(
        initValue = 0.001,
        label = "Learning rate",
        description = "Adam learning rate for the tape trainer",
        min = 0.0,
        increment = 0.0005,
        order = 1,
        setter = { value ->
            field = value
            baseObject.trainer.learningRate = value.toFloat()
        },
    )

    var samplingTemperature by GuiEditable(
        initValue = 1.0,
        label = "Sampling temperature",
        description = "Sharpens (below 1) or flattens (above 1) the next-token distribution before sampling",
        min = 0.01,
        max = 4.0,
        increment = 0.05,
        order = 2,
    )

    var diagramScale by GuiEditable(
        initValue = 1.0,
        label = "Diagram scale",
        description = "Multiplies interior tile sizes and spacing; labels and glyphs keep their point size, " +
            "so shrinking the diagram makes it denser without making text smaller",
        min = 0.25,
        max = 2.0,
        increment = 0.25,
        order = 3,
    )

    override var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.Greedy,
        label = "Sampling strategy",
        description = "How the next token is chosen from the distribution",
        showDetails = false,
        order = 5,
    )

    /** Splits prompt and injected text into vocabulary words; share the corpus tokenizer. */
    var tokenizer: Tokenizer<*> = SimpleTokenizer()

    @Transient
    private var appliedDiagramScale = 1.0

    override fun onCommit() {
        trainer.learningRate = learningRate.toFloat()
        if (diagramScale != appliedDiagramScale) {
            tileLayout = null
            junctionLayout = null
            rebuildScene()
            events.modelRebuilt.fire()
        }
    }

    /** Swaps tiles with gradient buffers to their backward view; forward values otherwise. */
    var gradientView: Boolean = false
        set(value) {
            field = value
            scene.setGradientView(value)
            scene.publish(currentSequenceRow())
        }

    /** Whether there is a computed gradient to show — false until a backward pass runs. */
    val hasGradients: Boolean get() = model.gradientsComputed

    /** The user's [gradientView] while a training walk's backward half auto-overrides it. */
    @Transient
    private var gradientViewBeforeWalk: Boolean? = null

    /**
     * Turns [gradientView] on for the backward half of a training walk and restores the user's
     * setting when the context reclaims the scene — the gradients stay up after the walk finishes,
     * alongside the training-window status, until the next forward pass on the context.
     */
    private fun autoGradientView(enable: Boolean) {
        if (enable) {
            if (gradientViewBeforeWalk == null) {
                gradientViewBeforeWalk = gradientView
                if (!gradientView) gradientView = true
            }
        } else {
            gradientViewBeforeWalk?.let { prior ->
                gradientViewBeforeWalk = null
                if (gradientView != prior) gradientView = prior
            }
        }
    }

    /** Whole-model head view from old saves; [deckSlices] supersedes it as the load fallback. */
    private var selectedHead: Int = 0

    /** Saved head slice per attention deck by tile id, applied to the scene on load. */
    var deckSlices: HashMap<String, Int>? = null

    /** Saved tile positions by tile id, applied to the scene on load. */
    var tileLayout: HashMap<String, DoubleArray>? = null

    /** Saved junction glyph centers by op name, applied to the scene on load. */
    var junctionLayout: HashMap<String, DoubleArray>? = null

    /** Saved position of the next-token probability card. */
    var probabilityCardLayout: DoubleArray? = null

    @Transient
    override var events: TinyLanguageModelEvents = TinyLanguageModelEvents()
        private set

    @Transient
    var scene: CompositorScene = TinyLmCompositor.buildScene(model)
        private set

    @Transient
    var trainer: TapeTrainer = TapeTrainer(model)
        private set

    @Transient
    private var windowCursor = 0

    /** Set while the scene shows a training window rather than the context; cleared by [forwardContext]. */
    @Transient
    private var sceneShowsTrainingWindow = false

    @Transient
    private var walkWindowOrdinal = 0

    @Transient
    private var walkWindowCount = 0

    @Transient
    private var walkWindowPreview: String? = null

    @Transient
    private var walkWindowTarget: String? = null

    /** Immutable display data for the next-token card; rebuilt after each completed forward pass. */
    @Transient
    @Volatile
    var tokenProbabilitySnapshot: TokenProbabilitySnapshot? = null
        private set

    constructor(config: TinyLmConfig) : this() {
        this.config = config
        rebuildRuntime(null)
    }

    private fun decks() = scene.tiles.filterIsInstance<DeckTile>()

    /** Rebuilds the headless model, trainer, and scene — after deserialization or a config change. */
    private fun rebuildRuntime(weights: Map<String, FloatArray>?) {
        model = TinyLmModel(config)
        weights?.forEach { (name, values) ->
            model.params[name]?.tensor?.takeIf { it.size == values.size }?.copyFrom(values)
        }
        // Only weights survive a rebuild; the gradients this view showed are gone.
        gradientView = false
        gradientViewBeforeWalk = null
        // Transient, so null mid-deserialization despite the type.
        @Suppress("SENSELESS_COMPARISON")
        if (trainer != null) trainer.job.cancel()
        trainer = TapeTrainer(model)
        trainer.learningRate = learningRate.toFloat()
        applyCorpusToTrainer()
        rebuildScene()
        events.modelRebuilt.fire()
    }

    private fun rebuildScene() {
        scene = TinyLmCompositor.buildScene(model, scale = diagramScale)
        appliedDiagramScale = diagramScale
        tileLayout?.forEach { (id, xy) ->
            scene.tiles.firstOrNull { it.id == id }?.let {
                it.x = xy[0]
                it.y = xy[1]
            }
        }
        junctionLayout?.forEach { (opName, xy) ->
            scene.opVertices.firstOrNull { it.op.name == opName }?.let {
                it.x = xy[0]
                it.y = xy[1]
                it.placed = true
            }
        }
        decks().forEach { it.selectedSlice = (deckSlices?.get(it.id) ?: selectedHead).coerceIn(0, it.slices - 1) }
        scene.setGradientView(gradientView)
        // A pager flip doesn't move any tile, so capture it directly rather than waiting for a layout change.
        scene.onHeadSelected = { _, _ -> captureViewState() }
    }

    /** Copies the scene's current tile positions and per-deck head slices into the serialized view state. */
    fun captureViewState() {
        tileLayout = scene.tiles.associateTo(HashMap()) { it.id to doubleArrayOf(it.x, it.y) }
        junctionLayout = scene.opVertices.associateTo(HashMap()) { it.op.name to doubleArrayOf(it.x, it.y) }
        deckSlices = decks().associateTo(HashMap()) { it.id to it.selectedSlice }
    }

    override fun appendNetworkDebugInfo(builder: StringBuilder, indent: String) {
        builder.appendLine("${indent}Model: context=${config.contextSize}, embedding=${config.embedDim}, " +
            "heads=${config.numHeads}, layers=${config.numLayers}, hidden=${config.hiddenDim}, vocabulary=${config.vocabSize}")
        builder.appendLine("${indent}Interior tiles (${scene.tiles.size}):")
        scene.tiles.forEach { tile ->
            builder.appendLine("${indent}  [${tile::class.simpleName}] ${tile.id} (${tile.title})  " +
                "rect: (${tile.x.roundToString(1)}, ${tile.y.roundToString(1)}) " +
                "${tile.width.roundToString(1)} x ${tile.height.roundToString(1)}")
        }
        builder.appendLine("${indent}Interior operations (${scene.opVertices.size}):")
        scene.opVertices.forEach { vertex ->
            builder.appendLine("${indent}  [${vertex.op::class.simpleName}] ${vertex.op.name}  " +
                "loc: (${vertex.x.roundToString(1)}, ${vertex.y.roundToString(1)})")
        }
    }

    /**
     * A full snapshot: the config, the trained parameters, the corpus, the context window, and
     * the settings and view state — everything serialization would carry. The copy trains and
     * generates independently of the original from the moment it is made.
     */
    fun copy(): TinyLanguageModel = TinyLanguageModel(config).also { copy ->
        synchronized(model) {
            model.params.forEach { (name, port) ->
                copy.model.params[name]?.tensor?.copyFrom(port.tensor.toFloatArray())
            }
            copy.contextTokens = contextTokens.copyOf()
        }
        copy.label = label
        copy.location = location
        copy.text = text
        copy.tokenizer = tokenizer.copy() as Tokenizer<*>
        copy.tokenLabels = tokenLabels?.let { ArrayList(it) }
        copy.corpusTokenIds = corpusTokenIds?.copyOf()
        copy.testCorpusTokenIds = testCorpusTokenIds?.copyOf()
        copy.applyCorpusToTrainer()
        copy.learningRate = learningRate
        copy.trainer.learningRate = learningRate.toFloat()
        copy.samplingTemperature = samplingTemperature
        copy.diagramScale = diagramScale
        copy.samplingStrategy = samplingStrategy.copy() as SamplingStrategy
        copy.deckSlices = decks().associateTo(HashMap()) { it.id to it.selectedSlice }
        copy.tileLayout = tileLayout?.mapValuesTo(HashMap()) { it.value.copyOf() }
        copy.junctionLayout = junctionLayout?.mapValuesTo(HashMap()) { it.value.copyOf() }
        copy.probabilityCardLayout = probabilityCardLayout?.copyOf()
        copy.rebuildScene()
    }

    /** Sets the training (and optional testing) corpus as vocabulary token ids. */
    fun setCorpus(tokenIds: IntArray, testTokenIds: IntArray? = null) {
        corpusTokenIds = tokenIds
        testCorpusTokenIds = testTokenIds
        applyCorpusToTrainer()
    }

    private fun applyCorpusToTrainer() {
        trainer.trainingWindows = corpusTokenIds?.let(::windowsOf) ?: emptyList()
        trainer.testingWindows = testCorpusTokenIds?.let(::windowsOf) ?: emptyList()
    }

    private fun windowsOf(corpus: IntArray): List<Pair<IntArray, IntArray>> {
        val seq = config.contextSize
        if (corpus.size <= seq) return emptyList()
        return (0 until corpus.size - seq).map { start ->
            corpus.copyOfRange(start, start + seq) to corpus.copyOfRange(start + 1, start + seq + 1)
        }
    }

    /** Replaces the context window (keeping the most recent [config.contextSize] tokens). */
    fun setContext(tokens: IntArray) {
        contextTokens = if (tokens.size <= config.contextSize) tokens.copyOf()
        else tokens.copyOfRange(tokens.size - config.contextSize, tokens.size)
    }

    context(Network)
    /** A walk in progress absorbs the iteration: finish to the clean boundary now, generate next tick. */
    override fun update() {
        if (stepWalkInProgress) {
            finishStepWalk()
            return
        }
        if (canAdvance) step() else forwardContext()
    }

    /** Maps text to vocabulary ids through [tokenizer] and [tokenLabels]; unknown words drop. */
    fun encode(textIn: String): IntArray {
        val labels = tokenLabels ?: return IntArray(0)
        val index = HashMap<String, Int>(labels.size)
        labels.forEachIndexed { i, label -> index.putIfAbsent(label, i) }
        return textIn.tokenize(tokenizer)
            .mapNotNull { index[it.token] ?: index[it.token.lowercase()] }
            .toIntArray()
    }

    fun decode(ids: IntArray): String {
        val labels = tokenLabels ?: return ""
        return tokenizer.joinTokens(ids.map { labels.getOrNull(it) }.filterNotNull())
    }

    override fun onClear() {
        contextTokens = IntArray(0)
    }

    override fun hasContinuation(): Boolean = contextTokens.isNotEmpty() || sampledToken >= 0

    /** Nothing to walk: the context is empty and nothing is queued, so it waits for text. */
    val waitingForInput: Boolean
        get() = !canAdvance

    /**
     * Advances generation by one token: slides the next pending word (or the last sample) into
     * the context, runs a full forward pass, and samples the next word. A mid-flight op walk is
     * completed by [update] before this runs; the guard here just protects direct calls. An empty
     * context waits for input, so playing before the document sync delivers text is not a dead end.
     */
    fun step(): Unit = synchronized(model) {
        if (trainer.isRunning) return
        lastGenerated = ""
        if (model.midWalk) return
        if (pending.isNotEmpty()) {
            setContext(contextTokens + pending.removeFirst())
            forwardContext()
            sampledToken = sampleNext()
            if (pending.isEmpty()) acceptSample()
            return
        }
        if (sampledToken >= 0) setContext(contextTokens + sampledToken)
        if (contextTokens.isEmpty()) return
        forwardContext()
        sampledToken = sampleNext()
        acceptSample()
    }

    private fun sampleNext(): Int {
        val distribution = nextTokenDistribution()
        val sample = sampleOverride?.invoke() ?: samplingStrategy.sample(distribution)
        tokenProbabilitySnapshot = TokenProbabilitySnapshot.full(distribution, sample)
        return sample
    }

    private fun acceptSample() {
        val label = tokenLabels?.getOrNull(sampledToken) ?: return
        lastGenerated = label
        text = if (text.isEmpty()) label else tokenizer.joinTokens(listOf(text, label))
        syncGate.invalidate()
    }

    /** The final residual stream row for the current context position. */
    override fun computeHiddenState(): DoubleArray {
        if (contextTokens.isEmpty()) return DoubleArray(0)
        val resid = model.plan.port("layers.${config.numLayers - 1}.resid").tensor
        val row = contextTokens.size - 1
        return DoubleArray(resid.cols) { resid[row, it].toDouble() }
    }

    override fun hiddenStateLabel() = "final residual"

    override fun encodeText(textIn: String): IntArray = encode(textIn)

    override fun onInjected(newText: String, ids: IntArray) {
        val injected = decode(ids)
        text = if (text.isEmpty()) injected else tokenizer.joinTokens(listOf(text, injected))
    }

    /** What the next forward pass will read: the context plus a sampled word not yet slid in. */
    private fun windowTokens(): IntArray {
        val withSample = if (pending.isEmpty() && sampledToken >= 0) {
            contextTokens + sampledToken
        } else contextTokens
        return if (withSample.size <= config.contextSize) withSample
        else withSample.copyOfRange(withSample.size - config.contextSize, withSample.size)
    }

    /**
     * The sliding window as text, so the coupled document visibly slides once the window
     * fills.
     */
    override fun windowText(): String = decode(windowTokens())

    /** Edits replace the context outright — the forward pass is stateless, nothing to replay. */
    override fun applyWindowEdit(ids: IntArray) {
        setContext(ids)
        pending = ArrayDeque()
        text = decode(contextTokens)
    }

    /** Runs a full forward pass on the current context and publishes it to the scene. */
    fun forwardContext(): Unit = synchronized(model) {
        if (trainer.isRunning) return
        if (contextTokens.isEmpty()) return
        if (model.midWalk) return
        sceneShowsTrainingWindow = false
        autoGradientView(false)
        model.setSample(contextTokens)
        model.forward()
        scene.lens?.sourceRow = contextTokens.size - 1
        scene.publish(currentSequenceRow())
        events.updated.fire()
    }

    /**
     * The model's distribution for the next token after the current context, with
     * [samplingTemperature] applied (probabilities raised to 1/T and renormalized).
     */
    fun nextTokenDistribution(): DoubleArray {
        val distribution = model.distributionAt((contextTokens.size - 1).coerceAtLeast(0))
        val temperature = samplingTemperature
        val powered = DoubleArray(distribution.size) {
            Math.pow(distribution[it].toDouble(), 1.0 / temperature)
        }
        val sum = powered.sum()
        if (sum > 0.0) for (i in powered.indices) powered[i] /= sum
        return powered
    }

    /**
     * Advances a micro-stepped training walk by one op, arming a fresh walk on the next training
     * window when idle. Returns the op that ran, or null with no training windows.
     */
    enum class StepRefusal {
        TRAINING_WALK_IN_PROGRESS, FORWARD_WALK_IN_PROGRESS, EMPTY_CONTEXT, NO_TRAINING_WINDOWS,
        NO_WALK_IN_PROGRESS, TRAINER_RUNNING
    }

    fun stepTrainingOp(): TensorOp? = synchronized(model) {
        if (model.stepPhase == TinyLmModel.StepPhase.IDLE) {
            // Walk-start guards only: finishStepWalk drains via this method, so a mid-walk
            // refusal would loop forever.
            if (trainer.isRunning) {
                events.stepRefused.fire(StepRefusal.TRAINER_RUNNING)
                return null
            }
            if (model.plan.cursor != 0) {
                events.stepRefused.fire(StepRefusal.FORWARD_WALK_IN_PROGRESS)
                return null
            }
            val windows = trainer.trainingWindows
            if (windows.isEmpty()) {
                events.stepRefused.fire(StepRefusal.NO_TRAINING_WINDOWS)
                return null
            }
            val (tokens, targets) = windows[windowCursor % windows.size]
            walkWindowOrdinal = windowCursor % windows.size + 1
            walkWindowCount = windows.size
            walkWindowPreview = decode(tokens)
            walkWindowTarget = decode(intArrayOf(targets.last()))
            sceneShowsTrainingWindow = true
            windowCursor++
            trainer.learningRate = learningRate.toFloat()
            autoGradientView(false)
            model.beginSteppedTrainStep(tokens, targets)
        }
        val op = model.stepOp()
        if (model.stepPhase == TinyLmModel.StepPhase.BACKWARD) autoGradientView(true)
        scene.lens?.sourceRow = config.contextSize - 1
        scene.publish(config.contextSize - 1)
        events.updated.fire()
        return op
    }

    /** Advances a plain forward pass on the current context by one op. */
    fun stepInferenceOp(): TensorOp? = synchronized(model) {
        if (model.stepPhase != TinyLmModel.StepPhase.IDLE) {
            events.stepRefused.fire(StepRefusal.TRAINING_WALK_IN_PROGRESS)
            return null
        }
        if (model.plan.cursor == 0) {
            if (trainer.isRunning) {
                events.stepRefused.fire(StepRefusal.TRAINER_RUNNING)
                return null
            }
            if (contextTokens.isEmpty()) {
                events.stepRefused.fire(StepRefusal.EMPTY_CONTEXT)
                return null
            }
            sceneShowsTrainingWindow = false
            autoGradientView(false)
            model.setSample(contextTokens)
            scene.lens?.sourceRow = contextTokens.size - 1
        }
        val op = model.stepForwardOnly()
        scene.publish(currentSequenceRow())
        events.updated.fire()
        return op
    }

    /** Runs the remaining ops of a walk in progress to the next clean boundary. */
    fun finishStepWalk(): Unit = synchronized(model) {
        if (!stepWalkInProgress) {
            events.stepRefused.fire(StepRefusal.NO_WALK_IN_PROGRESS)
            return
        }
        while (model.stepPhase != TinyLmModel.StepPhase.IDLE) stepTrainingOp()
        while (model.plan.cursor != 0) stepInferenceOp()
    }

    /** True while an op micro-step walk (training or forward-only) is mid-flight. */
    val stepWalkInProgress: Boolean
        get() = model.midWalk

    /** The op the next micro-step will run: mid-walk, mid-forward, or null at a clean boundary. */
    fun pendingOp(): TensorOp? = model.nextOp()
        ?: if (model.plan.cursor != 0) model.plan.ops[model.plan.cursor] else null

    /**
     * One line saying what the diagram is showing and where a step walk is: the walk's data source
     * (context, or which training window with its continuation target — the targets are the window
     * shifted by one, so window plus final target is the whole training pair) plus op-level
     * progress. Stays up after a training walk finishes — the tiles keep showing that window until
     * the next forward pass on the context clears it. Null when the scene shows the plain context
     * at rest.
     */
    fun stepStatusText(): String? {
        val source = if (sceneShowsTrainingWindow) {
            // Elide the front so the target stays adjacent to the text it continues.
            val preview = walkWindowPreview?.let { if (it.length > 30) "…" + it.takeLast(30) else it }
            val target = walkWindowTarget
            buildString {
                append("training window $walkWindowOrdinal/$walkWindowCount")
                if (!preview.isNullOrEmpty()) {
                    append(" “$preview”")
                    if (!target.isNullOrEmpty()) append(" → “$target”")
                }
            }
        } else "context"
        val opCount = model.plan.ops.size
        return when (model.stepPhase) {
            TinyLmModel.StepPhase.FORWARD ->
                "$source — forward op ${model.plan.cursor + 1}/$opCount"
            TinyLmModel.StepPhase.BACKWARD ->
                "$source — backward op ${model.tape.backwardStepNumber}/${model.tape.size}"
            TinyLmModel.StepPhase.IDLE -> when {
                model.plan.cursor != 0 -> "$source — forward op ${model.plan.cursor + 1}/$opCount"
                sceneShowsTrainingWindow -> "$source — trained, weights updated"
                else -> null
            }
        }
    }

    private fun currentSequenceRow() = (contextTokens.size - 1)
        .takeIf { it >= 0 } ?: config.contextSize - 1

    override suspend fun onDelete() {
        trainer.stopTraining()
    }

    fun readResolve(): Any {
        events = TinyLanguageModelEvents()
        return this
    }

    override fun toString(): String = buildString {
        appendLine("Name: $displayName (tiny language model)")
        appendLine("Config: context ${config.contextSize}, embed ${config.embedDim}, " +
                "${config.numHeads} heads, ${config.numLayers} layer(s), vocab ${config.vocabSize}")
        append("Trained iterations: ${trainer.iteration}")
    }

    class CreationTemplate : EditableObject {

        @UserParameter(label = "Context size", description = "Tokens in the context window", order = 1)
        var contextSize = 24

        @UserParameter(label = "Embedding dimension", description = "Width of the residual stream", order = 2)
        var embedDim = 20

        @UserParameter(label = "Attention heads", description = "Must divide the embedding dimension", order = 3)
        var numHeads = 4

        @UserParameter(label = "Hidden size", description = "MLP hidden units", order = 4)
        var hiddenDim = 30

        @UserParameter(label = "Vocabulary size", description = "Number of distinct tokens", order = 5)
        var vocabSize = 100

        @UserParameter(label = "Layers", description = "Transformer layers", order = 6)
        var numLayers = 1

        fun create(): TinyLanguageModel = TinyLanguageModel(TinyLmConfig(
            contextSize = contextSize,
            embedDim = embedDim,
            numHeads = numHeads,
            hiddenDim = hiddenDim,
            vocabSize = vocabSize,
            numLayers = numLayers,
        ))

        override val name = "Tiny Language Model"
    }

    companion object : WithXStreamPropertyConverter {

        /**
         * Serializes the [model] property as its parameter tensors (name to base64 floats),
         * captured live at marshal time; on unmarshal the runtime is rebuilt from the restored
         * config and the saved weights are copied back in.
         */
        override val xStreamPropertyConverter = createXStreamPropertyConverter<TinyLanguageModel>(
            marshal = {
                on(TinyLanguageModel::model) { writer, _ ->
                    writer.startNode("model")
                    params.forEach { (name, port) ->
                        writer.startNode("param")
                        writer.startNode("name"); writer.setValue(name); writer.endNode()
                        writer.startNode("values")
                        writer.setValue(floatsToBase64(port.tensor.toFloatArray()))
                        writer.endNode()
                        writer.endNode()
                    }
                    writer.endNode()
                }
            },
            unmarshal = {
                on("model") { reader, _ ->
                    val weights = HashMap<String, FloatArray>()
                    while (reader.hasMoreChildren()) {
                        reader.moveDown()
                        var name: String? = null
                        var values: FloatArray? = null
                        while (reader.hasMoreChildren()) {
                            reader.moveDown()
                            when (reader.nodeName) {
                                "name" -> name = reader.value
                                "values" -> values = base64ToFloats(reader.value)
                            }
                            reader.moveUp()
                        }
                        if (name != null && values != null) weights[name] = values
                        reader.moveUp()
                    }
                    withConstructedObject { rebuildRuntime(weights) }
                }
            }
        )

        private fun floatsToBase64(values: FloatArray): String {
            val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES)
            buffer.asFloatBuffer().put(values)
            return Base64.getEncoder().encodeToString(buffer.array())
        }

        private fun base64ToFloats(encoded: String): FloatArray {
            val bytes = Base64.getDecoder().decode(encoded)
            val floats = FloatArray(bytes.size / Float.SIZE_BYTES)
            ByteBuffer.wrap(bytes).asFloatBuffer().get(floats)
            return floats
        }
    }
}
