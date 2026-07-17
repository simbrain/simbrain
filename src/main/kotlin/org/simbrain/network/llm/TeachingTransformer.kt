package org.simbrain.network.llm

import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.DeckTile
import org.simbrain.network.compositor.TeachingCompositor
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.TensorRole
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.network.trainers.TapeTrainer
import org.simbrain.util.SimpleTokenizer
import org.simbrain.util.Tokenizer
import org.simbrain.util.UserParameter
import org.simbrain.util.WithXStreamPropertyConverter
import org.simbrain.util.createXStreamPropertyConverter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.tokenize
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D
import java.nio.ByteBuffer
import java.util.Base64
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.BiasOp
import org.simbrain.network.tensor.op.CausalMaskedRowSoftmaxOp
import org.simbrain.network.tensor.op.Gradients
import org.simbrain.network.tensor.op.HeadMixOp
import org.simbrain.network.tensor.op.HeadScoresOp
import org.simbrain.network.tensor.op.LayerNormOp
import org.simbrain.network.tensor.op.MatMulLinearOp
import org.simbrain.network.tensor.op.MergeHeadsOp
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.ReLUOp
import org.simbrain.network.tensor.op.SeqEmbedOp
import org.simbrain.network.tensor.op.SeqSoftmaxCrossEntropyOp
import org.simbrain.network.tensor.op.SplitHeadsOp
import org.simbrain.network.tensor.op.Tape
import org.simbrain.network.tensor.op.TensorAdam
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.network.tensor.op.TensorPort
import kotlin.math.sqrt
import kotlin.random.Random

data class TeachingTransformerConfig(
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
 * Headless GPT-style teaching transformer, expressed as one explicit [OpPlan] spanning
 * embed -> (attention -> MLP) x numLayers -> unembed -> sequence cross-entropy. There is no
 * block class: a "layer" is a `layers.<l>.*` range of port names, so [org.simbrain.network.tensor.op.OpPlan.cursor]
 * micro-steps the whole model and one [Tape] records the full loss-to-param path.
 *
 * Unlike [Lfm2Model] this processes the FULL context each forward (no caches) and owns its
 * randomly initialized parameters, trained in place via [Tape] + [TensorAdam]. Pre-norm
 * residuals: the residual trunk is a pure identity path (the visual spine's skip connection);
 * layer norm sits at the entry of each attention/MLP limb.
 */
class TeachingTransformerModel(val config: TeachingTransformerConfig, seed: Long = 42L) {

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
        ops += embedOp
        var resid = workspace("resid0", seq, dim)
        ops += AddOp("add_pos", embedded, posTable, resid)

        for (l in 0 until c.numLayers) {
            val prefix = "layers.$l"

            val attnNormed = workspace("$prefix.attn.normed", seq, dim)
            ops += LayerNormOp("$prefix.attn.norm", resid,
                onesParam("$prefix.attn.norm.gamma", dim), zerosParam("$prefix.attn.norm.beta", dim),
                attnNormed, c.normEps)
            val q = workspace("$prefix.attn.q", seq, dim)
            val k = workspace("$prefix.attn.k", seq, dim)
            val v = workspace("$prefix.attn.v", seq, dim)
            ops += MatMulLinearOp("$prefix.attn.q_proj", weightParam("$prefix.attn.wq", dim, dim), attnNormed, q)
            ops += MatMulLinearOp("$prefix.attn.k_proj", weightParam("$prefix.attn.wk", dim, dim), attnNormed, k)
            ops += MatMulLinearOp("$prefix.attn.v_proj", weightParam("$prefix.attn.wv", dim, dim), attnNormed, v)
            val qHeads = workspace("$prefix.attn.q_heads", c.numHeads * seq, c.headDim)
            val kHeads = workspace("$prefix.attn.k_heads", c.numHeads * seq, c.headDim)
            val vHeads = workspace("$prefix.attn.v_heads", c.numHeads * seq, c.headDim)
            ops += SplitHeadsOp("$prefix.attn.q_split", q, qHeads, c.numHeads)
            ops += SplitHeadsOp("$prefix.attn.k_split", k, kHeads, c.numHeads)
            ops += SplitHeadsOp("$prefix.attn.v_split", v, vHeads, c.numHeads)
            val scores = workspace("$prefix.attn.scores", c.numHeads * seq, seq)
            ops += HeadScoresOp("$prefix.attn.score", qHeads, kHeads, scores, c.numHeads)
            val attnWeights = workspace("$prefix.attn.weights", c.numHeads * seq, seq)
            ops += CausalMaskedRowSoftmaxOp("$prefix.attn.softmax", scores, attnWeights, c.numHeads)
            val mixed = workspace("$prefix.attn.mixed", c.numHeads * seq, c.headDim)
            ops += HeadMixOp("$prefix.attn.mix", attnWeights, vHeads, mixed, c.numHeads)
            val merged = workspace("$prefix.attn.merged", seq, dim)
            ops += MergeHeadsOp("$prefix.attn.merge", mixed, merged, c.numHeads)
            val attnOut = workspace("$prefix.attn.out", seq, dim)
            ops += MatMulLinearOp("$prefix.attn.out_proj", weightParam("$prefix.attn.wo", dim, dim), merged, attnOut)
            val attnResid = workspace("$prefix.attn_resid", seq, dim)
            ops += AddOp("$prefix.attn_residual", resid, attnOut, attnResid)

            val mlpNormed = workspace("$prefix.mlp.normed", seq, dim)
            ops += LayerNormOp("$prefix.mlp.norm", attnResid,
                onesParam("$prefix.mlp.norm.gamma", dim), zerosParam("$prefix.mlp.norm.beta", dim),
                mlpNormed, c.normEps)
            val hiddenRaw = workspace("$prefix.mlp.hidden_raw", seq, c.hiddenDim)
            val hidden = workspace("$prefix.mlp.hidden", seq, c.hiddenDim)
            val act = workspace("$prefix.mlp.act", seq, c.hiddenDim)
            val outRaw = workspace("$prefix.mlp.out_raw", seq, dim)
            val mlpOut = workspace("$prefix.mlp.out", seq, dim)
            ops += MatMulLinearOp("$prefix.mlp.up_proj", weightParam("$prefix.mlp.w1", c.hiddenDim, dim), mlpNormed, hiddenRaw)
            ops += BiasOp("$prefix.mlp.up_bias", hiddenRaw, zerosParam("$prefix.mlp.b1", c.hiddenDim), hidden)
            ops += ReLUOp("$prefix.mlp.relu", hidden, act)
            ops += MatMulLinearOp("$prefix.mlp.down_proj", weightParam("$prefix.mlp.w2", dim, c.hiddenDim), act, outRaw)
            ops += BiasOp("$prefix.mlp.down_bias", outRaw, zerosParam("$prefix.mlp.b2", dim), mlpOut)
            val layerResid = workspace("$prefix.resid", seq, dim)
            ops += AddOp("$prefix.residual", attnResid, mlpOut, layerResid)
            resid = layerResid
        }

        val finalNormed = workspace("final_normed", seq, dim)
        ops += LayerNormOp("final_norm", resid,
            onesParam("final_norm.gamma", dim), zerosParam("final_norm.beta", dim),
            finalNormed, c.normEps)
        val logits = workspace("logits", seq, c.vocabSize)
        ops += MatMulLinearOp("unembed", weightParam("unembed.weight", c.vocabSize, dim), finalNormed, logits)
        ceOp = SeqSoftmaxCrossEntropyOp("cross_entropy", logits,
            workspace("probs", seq, c.vocabSize), workspace("loss", 1, 1))
        ops += ceOp

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

    /** One tape-recorded forward + backward + Adam update on [tokens]/[targets]; returns the loss. */
    @Synchronized
    fun trainStep(tokens: IntArray, targets: IntArray): Float {
        setSample(tokens, targets)
        tape.clear()
        grads.zeroAll()
        plan.forward(tape)
        val lossValue = loss.tensor.data.get(0)
        tape.backward(loss, grads)
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

class TeachingTransformerEvents : LocationEvents() {
    val modelRebuilt = NoArgEvent()
}

/**
 * The teaching transformer on the network canvas: wraps the headless [TeachingTransformerModel],
 * its [TapeTrainer], and the compositor spine scene. Each network update runs a full forward pass
 * on the current context, so the workspace drives generation while the interior shows the whole
 * computation.
 *
 * Serialization: the config, corpus, view state, AND the trained weights go into the workspace
 * file — this is a trained-in-place teaching artifact, unlike [LanguageModel], which reloads its
 * weights from disk. Weight capture happens at marshal time through the companion's property
 * converter, so whatever training did up to the save is what comes back.
 *
 * As an [AttributeContainer] it exposes the same coupling vocabulary as [LanguageModel] —
 * [generatedToken] and [hiddenState] out, [injectText] and the [contextWindow] document sync
 * in — over a word-level vocabulary: text maps through [tokenizer] and [tokenLabels], and
 * words outside the vocabulary are dropped.
 */
class TeachingTransformer @XStreamConstructor constructor() : LocatableModel(), EditableObject, AttributeContainer {

    var config: TeachingTransformerConfig = TeachingTransformerConfig()
        private set

    var model: TeachingTransformerModel = TeachingTransformerModel(config)
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

    var prompt by GuiEditable(
        initValue = "",
        label = "Prompt",
        description = "Text generation restarts from; words outside the vocabulary are dropped",
        order = 4,
    )

    var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.Greedy,
        label = "Sampling strategy",
        description = "How the next token is chosen from the distribution",
        showDetails = false,
        order = 5,
    )

    /** Splits prompt and injected text into vocabulary words; share the corpus tokenizer. */
    var tokenizer: Tokenizer<*> = SimpleTokenizer()

    /** Prompt plus generated continuation from the current run; keeps the full history. */
    var text: String = ""
        private set

    @Transient
    var isGenerating = false
        private set

    /** Vocabulary ids waiting to enter the context, one per iteration: prompt, then injections. */
    @Transient
    private var pending = ArrayDeque<Int>()

    @Transient
    private var sampledToken = -1

    @Transient
    private var lastGenerated = ""

    @Transient
    private var syncGate = DocumentSyncGate()

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

    var lensEnabled: Boolean = true
        set(value) {
            field = value
            scene.lens?.enabled = value
        }

    var selectedHead: Int = 0
        set(value) {
            field = value
            decks().forEach { it.selectedSlice = value.coerceIn(0, it.slices - 1) }
        }

    /** Saved tile positions by tile id, applied to the scene on load. */
    var tileLayout: HashMap<String, DoubleArray>? = null

    /** Saved junction glyph centers by op name, applied to the scene on load. */
    var junctionLayout: HashMap<String, DoubleArray>? = null

    override var location: Point2D = Point2D.Double()
        set(value) {
            field = value
            events.locationChanged.fire()
        }

    @Transient
    override var events: TeachingTransformerEvents = TeachingTransformerEvents()
        private set

    @Transient
    var scene: CompositorScene = TeachingCompositor.buildScene(model)
        private set

    @Transient
    var trainer: TapeTrainer = TapeTrainer(model)
        private set

    @Transient
    private var windowCursor = 0

    constructor(config: TeachingTransformerConfig) : this() {
        this.config = config
        rebuildRuntime(null)
    }

    private fun decks() = scene.tiles.filterIsInstance<DeckTile>()

    /** Rebuilds the headless model, trainer, and scene — after deserialization or a config change. */
    private fun rebuildRuntime(weights: Map<String, FloatArray>?) {
        model = TeachingTransformerModel(config)
        weights?.forEach { (name, values) ->
            model.params[name]?.tensor?.takeIf { it.size == values.size }?.copyFrom(values)
        }
        trainer = TapeTrainer(model)
        trainer.learningRate = learningRate.toFloat()
        applyCorpusToTrainer()
        rebuildScene()
        events.modelRebuilt.fire()
    }

    private fun rebuildScene() {
        scene = TeachingCompositor.buildScene(model, scale = diagramScale)
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
        decks().forEach { it.selectedSlice = selectedHead.coerceIn(0, it.slices - 1) }
        scene.lens?.enabled = lensEnabled
    }

    /** Copies the scene's current tile positions and deck slice into the serialized view state. */
    fun captureViewState() {
        tileLayout = scene.tiles.associateTo(HashMap()) { it.id to doubleArrayOf(it.x, it.y) }
        junctionLayout = scene.opVertices.associateTo(HashMap()) { it.op.name to doubleArrayOf(it.x, it.y) }
        decks().firstOrNull()?.let { selectedHead = it.selectedSlice }
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
    override fun update() {
        if (isGenerating) step() else forwardContext()
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

    /** Clears the context and starts a fresh generation run from [prompt]. */
    @Synchronized
    fun startGeneration() {
        val ids = encode(prompt)
        pending = ArrayDeque(ids.toList())
        contextTokens = IntArray(0)
        sampledToken = -1
        lastGenerated = ""
        text = decode(ids)
        syncGate.reset()
        isGenerating = true
        events.updated.fire()
    }

    @Synchronized
    fun stopGeneration() {
        isGenerating = false
    }

    /** Continues a stopped run from the current context, or starts fresh if there is none. */
    @Synchronized
    fun resumeGeneration() {
        if (pending.isEmpty() && contextTokens.isEmpty()) {
            startGeneration()
            return
        }
        isGenerating = true
        events.updated.fire()
    }

    /**
     * Advances generation by one token: slides the next pending word (or the last sample) into
     * the context, runs a full forward pass, and samples the next word. Skips the iteration
     * while an op micro-step walk is mid-flight.
     */
    @Synchronized
    fun step() {
        lastGenerated = ""
        if (!isGenerating) return
        if (model.stepPhase != TeachingTransformerModel.StepPhase.IDLE || model.plan.cursor != 0) return
        if (pending.isNotEmpty()) {
            setContext(contextTokens + pending.removeFirst())
            forwardContext()
            sampledToken = samplingStrategy.sample(nextTokenDistribution())
            if (pending.isEmpty()) acceptSample()
            return
        }
        if (sampledToken >= 0) setContext(contextTokens + sampledToken)
        if (contextTokens.isEmpty()) {
            isGenerating = false
            return
        }
        forwardContext()
        sampledToken = samplingStrategy.sample(nextTokenDistribution())
        acceptSample()
    }

    private fun acceptSample() {
        val label = tokenLabels?.getOrNull(sampledToken) ?: return
        lastGenerated = label
        text = if (text.isEmpty()) label else tokenizer.joinTokens(listOf(text, label))
        syncGate.invalidate()
    }

    /** Text of the word generated this iteration; empty while walking queued text or stopped. */
    @get:Producible
    val generatedToken: String
        get() = lastGenerated

    /** The final residual stream row for the current context position. */
    @get:Producible(customDescriptionMethod = "hiddenStateDescription")
    val hiddenState: DoubleArray
        get() {
            if (contextTokens.isEmpty()) return DoubleArray(0)
            val resid = model.plan.port("layers.${config.numLayers - 1}.resid").tensor
            val row = contextTokens.size - 1
            return DoubleArray(resid.cols) { resid[row, it].toDouble() }
        }

    fun hiddenStateDescription() = "$id:hiddenState (final residual)"

    /**
     * Queues [newText]'s vocabulary words to enter the context ahead of the model's own
     * continuation; a freshly sampled word that has not entered the context yet goes first,
     * keeping [text]'s order. Does not start or resume a stopped run.
     */
    @Synchronized
    @Consumable
    fun injectText(newText: String) {
        if (newText.isEmpty()) return
        val ids = encode(newText)
        if (ids.isEmpty()) return
        if (pending.isEmpty() && sampledToken >= 0) pending.addLast(sampledToken)
        ids.forEach { pending.addLast(it) }
        syncGate.invalidate()
        val injected = decode(ids)
        text = if (text.isEmpty()) injected else tokenizer.joinTokens(listOf(text, injected))
    }

    /**
     * The sliding context window as text — exactly what the next forward pass reads, so the
     * document visibly slides once the window fills. Follows the same sync protocol as
     * [LanguageModel.contextWindow]; consumed edits replace the context outright, which is
     * free here — the forward pass is stateless, so there is nothing to replay.
     */
    /** What the next forward pass will read: the context plus a sampled word not yet slid in. */
    private fun windowTokens(): IntArray {
        val withSample = if (pending.isEmpty() && sampledToken >= 0) {
            contextTokens + sampledToken
        } else contextTokens
        return if (withSample.size <= config.contextSize) withSample
        else withSample.copyOfRange(withSample.size - config.contextSize, withSample.size)
    }

    @get:Producible
    @set:Consumable
    var contextWindow: String
        @Synchronized
        get() = syncGate.publish(decode(windowTokens()), isGenerating)
        @Synchronized
        set(value) {
            if (!syncGate.isEdit(value, decode(windowTokens()), isGenerating)) return
            val ids = encode(value)
            if (ids.isEmpty()) return
            setContext(ids)
            pending = ArrayDeque()
            sampledToken = -1
            lastGenerated = ""
            syncGate.invalidate()
            text = decode(contextTokens)
            events.updated.fire()
        }

    /** Runs a full forward pass on the current context and publishes it to the scene. */
    fun forwardContext() {
        if (contextTokens.isEmpty()) return
        if (model.stepPhase != TeachingTransformerModel.StepPhase.IDLE || model.plan.cursor != 0) return
        model.setSample(contextTokens)
        model.forward()
        scene.lens?.sourceRow = contextTokens.size - 1
        scene.publish()
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
    fun stepTrainingOp(): TensorOp? {
        if (model.stepPhase == TeachingTransformerModel.StepPhase.IDLE) {
            val windows = trainer.trainingWindows
            if (windows.isEmpty()) return null
            val (tokens, targets) = windows[windowCursor % windows.size]
            windowCursor++
            trainer.learningRate = learningRate.toFloat()
            model.beginSteppedTrainStep(tokens, targets)
        }
        val op = model.stepOp()
        scene.publish()
        events.updated.fire()
        return op
    }

    /** Advances a plain forward pass on the current context by one op. */
    fun stepInferenceOp(): TensorOp? {
        if (model.stepPhase != TeachingTransformerModel.StepPhase.IDLE) return null
        if (model.plan.cursor == 0) {
            if (contextTokens.isEmpty()) return null
            model.setSample(contextTokens)
            scene.lens?.sourceRow = contextTokens.size - 1
        }
        val op = model.stepForwardOnly()
        scene.publish()
        events.updated.fire()
        return op
    }

    /** The op the next micro-step will run: mid-walk, mid-forward, or null at a clean boundary. */
    fun pendingOp(): TensorOp? = model.nextOp()
        ?: if (model.plan.cursor != 0) model.plan.ops[model.plan.cursor] else null

    override suspend fun delete(): List<NetworkModel> {
        stopGeneration()
        trainer.stopTraining()
        events.deleted.fire(this)
        return listOf(this)
    }

    fun readResolve(): Any {
        events = TeachingTransformerEvents()
        return this
    }

    override fun toString(): String = buildString {
        appendLine("Name: $displayName (teaching transformer)")
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

        fun create(): TeachingTransformer = TeachingTransformer(TeachingTransformerConfig(
            contextSize = contextSize,
            embedDim = embedDim,
            numHeads = numHeads,
            hiddenDim = hiddenDim,
            vocabSize = vocabSize,
            numLayers = numLayers,
        ))

        override val name = "Teaching Transformer"
    }

    companion object : WithXStreamPropertyConverter {

        /**
         * Serializes the [model] property as its parameter tensors (name to base64 floats),
         * captured live at marshal time; on unmarshal the runtime is rebuilt from the restored
         * config and the saved weights are copied back in.
         */
        override val xStreamPropertyConverter = createXStreamPropertyConverter<TeachingTransformer>(
            marshal = {
                on(TeachingTransformer::model) { writer, _ ->
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
