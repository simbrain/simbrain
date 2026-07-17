package org.simbrain.network.llm

import org.simbrain.network.compositor.AttentionTile
import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.Lfm2StackCompositor
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.tensor.Blas
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D
import java.nio.file.Path
import kotlin.math.exp

class LanguageModelEvents : LocationEvents() {
    val weightsLoaded = NoArgEvent()
}

enum class PromptMode(private val label: String) {
    COMPLETION("Completion"),
    CHAT("Chat");

    override fun toString() = label
}

/**
 * A language model on the network canvas: wraps the headless [Lfm2Model] and exposes its interior
 * through a [CompositorScene]. One network update generates one token, so the workspace play
 * button steps generation token by token. Loading weights arms generation automatically; a run
 * continues until the context window fills, the model emits end-of-text, or an optional token
 * budget is reached, and [startGeneration] resets the context window for a fresh run.
 *
 * Only the weights directory, config, prompt, sampling parameters, and view state are serialized —
 * never the weights. On deserialization the model reloads from [weightsDirectory]; if the files
 * are missing the model stays unloaded and the GUI offers to relocate or download them.
 *
 * As an [AttributeContainer] the model couples to other components through plain strings and
 * arrays: [generatedToken] and [hiddenState] out, [injectText] in. Everything needing the
 * tokenizer or unembedding stays on this side of the coupling. All attributes are safe to call
 * while weights are unloaded (empty results, no-op consumption).
 */
class LanguageModel @XStreamConstructor constructor() : LocatableModel(), EditableObject, AttributeContainer {

    /** Directory containing `model.safetensors` and `tokenizer.json`. */
    var weightsDirectory: String = ""

    var maxSeqLen: Int = 512
        private set

    var prompt by GuiEditable(
        initValue = "The capital of France is",
        label = "Prompt",
        description = "Text the model continues from. Generation restarts from this prompt.",
        order = 1,
    )

    var promptMode: PromptMode by GuiEditable(
        initValue = PromptMode.COMPLETION,
        label = "Prompt mode",
        description = "Completion continues the prompt verbatim; " +
            "chat wraps it as a user message the model answers",
        order = 2,
    )

    var systemPrompt by GuiEditable(
        initValue = "",
        label = "System prompt",
        description = "Optional system message ahead of the user message; chat mode only",
        order = 3,
    )

    var tokensToGenerate by GuiEditable(
        initValue = 0,
        label = "Tokens to generate",
        description = "Generation stops after this many tokens beyond the prompt; " +
            "0 generates until the context window fills",
        min = 0,
        order = 4,
    )

    var temperature by GuiEditable(
        initValue = 1.0,
        label = "Temperature",
        description = "Softmax temperature applied to the logits before sampling",
        min = 0.01,
        max = 4.0,
        increment = 0.05,
        order = 5,
    )

    var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.Greedy,
        label = "Sampling strategy",
        description = "How the next token is chosen from the distribution",
        showDetails = false,
        order = 6,
    )

    var stopAtEndOfText by GuiEditable(
        initValue = true,
        label = "Stop at end of text",
        description = "End the run when the model emits its end-of-text token; " +
            "chat mode always stops there",
        order = 7,
    )

    /** The model layer the structure view shows; the depth strip is the live selector. */
    var selectedLayer: Int = 0

    var selectedHead: Int = 0
        set(value) {
            field = value
            attentionTile?.selectedHead = value
        }

    var lensEnabled: Boolean = true
        set(value) {
            field = value
            loaded?.scene?.lens?.enabled = value
        }

    /** Saved tile positions by tile id, applied to the scene on load. */
    var tileLayout: HashMap<String, DoubleArray>? = null

    /** Prompt plus generated continuation from the current run. */
    var text: String = ""
        private set

    override var location: Point2D = Point2D.Double()
        set(value) {
            field = value
            events.locationChanged.fire()
        }

    @Transient
    override var events: LanguageModelEvents = LanguageModelEvents()
        private set

    class LoadedState(val model: Lfm2Model, val tokenizer: LlmTokenizer, var scene: CompositorScene)

    @Transient
    var loaded: LoadedState? = null
        private set

    val isLoaded get() = loaded != null

    /**
     * Tokens waiting to be fed, one per iteration: the encoded prompt after [startGeneration],
     * plus anything [injectText] appends. While non-empty the model is prefilling; once drained
     * it feeds back its own [sampledToken].
     */
    @Transient
    private var pending = ArrayDeque<Int>()

    @Transient
    private var generatedCount = 0

    @Transient
    private var sampledToken = -1

    @Transient
    private var lastGenerated = ""

    /** The committed token stream in [text] order: prompt, injections, accepted samples. */
    @Transient
    private var windowIds = ArrayList<Int>()

    @Transient
    private var syncGate = DocumentSyncGate()

    @Transient
    var isGenerating = false
        private set

    private val attentionTile
        get() = loaded?.scene?.tiles?.firstOrNull { it.id == "block.attn.weights" } as? AttentionTile

    constructor(weightsDirectory: String, maxSeqLen: Int = 512) : this() {
        this.weightsDirectory = weightsDirectory
        this.maxSeqLen = maxSeqLen
    }

    /**
     * Loads weights and tokenizer from [weightsDirectory] and builds the compositor scene. Heavy
     * (~1 GB of tensors, a few seconds) — call off the EDT. Throws if the directory is invalid.
     */
    @Synchronized
    fun loadWeights() {
        val dir = Path.of(weightsDirectory)
        check(Lfm2Weights.isValidWeightsDirectory(dir)) {
            "No model.safetensors and tokenizer.json in $weightsDirectory"
        }
        Blas.numThreads = 4
        val model = Lfm2Model(Lfm2Config(maxSeqLen = maxSeqLen), Safetensors.load(dir.resolve("model.safetensors")))
        val tokenizer = LlmTokenizer(dir.resolve("tokenizer.json"))
        loaded = LoadedState(model, tokenizer, buildScene(model))
        startGeneration()
        events.weightsLoaded.fire()
    }

    private fun buildScene(model: Lfm2Model): CompositorScene {
        val scene = Lfm2StackCompositor.buildScene(model)
        tileLayout?.forEach { (id, xy) ->
            scene.tiles.firstOrNull { it.id == id }?.let {
                it.x = xy[0]
                it.y = xy[1]
            }
        }
        (scene.tiles.firstOrNull { it.id == "block.attn.weights" } as? AttentionTile)
            ?.selectedHead = selectedHead
        scene.lens?.apply {
            enabled = lensEnabled
            async = true
            onReadingsUpdated = { events.updateGraphics.fire() }
        }
        val select = scene.layerSelector
        scene.layerSelector = { layer ->
            select?.invoke(layer)
            selectedLayer = scene.selectedLayer
        }
        scene.layerSelector?.invoke(selectedLayer)
        return scene
    }

    /** Copies the scene's current tile positions into [tileLayout] so they serialize. */
    fun captureViewState() {
        val scene = loaded?.scene ?: return
        tileLayout = scene.tiles.associateTo(HashMap()) { it.id to doubleArrayOf(it.x, it.y) }
    }

    /** Resets the model's context window and starts a fresh generation run from [prompt]. */
    @Synchronized
    fun startGeneration() {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        val ids = encodePrompt(state.tokenizer)
        pending = ArrayDeque(ids.toList())
        windowIds = ArrayList(ids.toList())
        syncGate.reset()
        generatedCount = 0
        sampledToken = -1
        lastGenerated = ""
        text = prompt
        isGenerating = true
        events.updated.fire()
    }

    @Synchronized
    fun stopGeneration() {
        isGenerating = false
    }

    /** Continues a stopped run where it left off; no-op when the context window is full. */
    @Synchronized
    fun resumeGeneration() {
        val state = loaded ?: return
        if (pending.isEmpty() && sampledToken < 0) {
            startGeneration()
            return
        }
        if (state.model.position >= state.model.config.maxSeqLen) return
        isGenerating = true
        events.updated.fire()
    }

    context(Network)
    override fun update() {
        step()
    }

    /**
     * Advances generation by one token: feeds the next pending token (or the last sampled token
     * once the queue is drained), publishes activations to the scene, and samples the next
     * token. No-op unless a run is active.
     */
    @Synchronized
    fun step() {
        val state = loaded ?: return
        lastGenerated = ""
        if (!isGenerating) return
        if (state.model.position >= state.model.config.maxSeqLen) {
            isGenerating = false
            return
        }
        val id = if (pending.isNotEmpty()) pending.removeFirst() else sampledToken
        if (id < 0) {
            isGenerating = false
            return
        }
        val position = state.model.position
        val logits = state.model.forwardToken(id)
        state.scene.publish(position)
        sampledToken = sampleToken(logits)
        if (pending.isEmpty()) {
            windowIds.add(sampledToken)
            syncGate.invalidate()
            val stopAtEos = stopAtEndOfText || promptMode == PromptMode.CHAT
            if (stopAtEos && sampledToken == state.model.config.eosTokenId) {
                isGenerating = false
            } else {
                lastGenerated = state.tokenizer.decode(
                    intArrayOf(sampledToken),
                    skipSpecials = promptMode == PromptMode.CHAT,
                )
                text += lastGenerated
                generatedCount++
                if (tokensToGenerate > 0 && generatedCount >= tokensToGenerate) {
                    isGenerating = false
                }
            }
        }
        events.updated.fire()
    }

    /** Text of the token generated this iteration; empty while prefilling or stopped. */
    @get:Producible
    val generatedToken: String
        get() = lastGenerated

    /** The residual stream at [selectedLayer] for the last processed token. */
    @get:Producible(customDescriptionMethod = "hiddenStateDescription")
    val hiddenState: DoubleArray
        get() {
            val state = loaded ?: return DoubleArray(0)
            val layer = selectedLayer.coerceIn(0, state.model.config.numLayers - 1)
            val tensor = state.model.plan.port("layers.$layer.resid").tensor
            return DoubleArray(tensor.size) { tensor.data.get(it).toDouble() }
        }

    fun hiddenStateDescription() = "$id:hiddenState (layer $selectedLayer residual)"

    /**
     * Encodes [newText] without adding specials and appends it to the feed queue, extending
     * prefill: the model walks the injected tokens one per iteration before resuming its own
     * continuation. A freshly sampled token that has not been fed yet is queued first, so the
     * context stays in [text]'s order. Does not start or resume a stopped run. Meant for
     * advancing sources — a static string producer re-injects its value every coupling update.
     */
    @Synchronized
    @Consumable
    fun injectText(newText: String) {
        val state = loaded ?: return
        if (newText.isEmpty()) return
        if (pending.isEmpty() && sampledToken >= 0) pending.addLast(sampledToken)
        state.tokenizer.encode(newText, addSpecials = false).forEach {
            pending.addLast(it)
            windowIds.add(it)
        }
        syncGate.invalidate()
        text += newText
    }

    /**
     * The full context window as text, scaffolding included: everything committed to the token
     * stream, decoded with specials kept. Producing follows an ownership rule — the window is
     * published while a run is generating (plus until a stopped run's final window has echoed
     * back), and is empty otherwise, so a paired document consumer is never clobbered while the
     * user may be editing it. Consuming an unrecognized value is an edit: the model resets and
     * requeues the whole edited window for a watchable re-prefill, preserving the run state
     * (a stopped model stays stopped until resumed).
     */
    @get:Producible
    @set:Consumable
    var contextWindow: String
        @Synchronized
        get() {
            val state = loaded ?: return ""
            return syncGate.publish(state.tokenizer.decode(windowIds.toIntArray()), isGenerating)
        }
        @Synchronized
        set(value) {
            val state = loaded ?: return
            val current = state.tokenizer.decode(windowIds.toIntArray())
            if (!syncGate.isEdit(value, current, isGenerating)) return
            state.model.reset()
            state.scene.reset()
            val ids = state.tokenizer.encode(value, addSpecials = false)
            pending = ArrayDeque(ids.toList())
            windowIds = ArrayList(ids.toList())
            syncGate.invalidate()
            generatedCount = 0
            sampledToken = -1
            lastGenerated = ""
            text = state.tokenizer.decode(ids, skipSpecials = true)
            events.updated.fire()
        }

    /**
     * Encodes [prompt] for a fresh run. Chat mode builds the full templated string — BOS text
     * included — and encodes with specials off, so the post-processor cannot add a second BOS.
     */
    private fun encodePrompt(tokenizer: LlmTokenizer): IntArray = when (promptMode) {
        PromptMode.COMPLETION -> tokenizer.encode(prompt)
        PromptMode.CHAT -> tokenizer.encode(
            Lfm2ChatFormat.chatPrompt(prompt, systemPrompt),
            addSpecials = false,
        )
    }

    private fun sampleToken(logits: FloatTensor): Int {
        val strategy = samplingStrategy
        if (strategy === SamplingStrategy.Greedy) {
            var best = 0
            for (i in 1 until logits.size) {
                if (logits.data.get(i) > logits.data.get(best)) best = i
            }
            return best
        }
        val probs = DoubleArray(logits.size)
        val t = temperature
        var max = Double.NEGATIVE_INFINITY
        for (i in probs.indices) {
            probs[i] = logits.data.get(i) / t
            if (probs[i] > max) max = probs[i]
        }
        var sum = 0.0
        for (i in probs.indices) {
            probs[i] = exp(probs[i] - max)
            sum += probs[i]
        }
        for (i in probs.indices) probs[i] /= sum
        return strategy.sample(probs)
    }

    override suspend fun delete(): List<NetworkModel> {
        stopGeneration()
        events.deleted.fire(this)
        return listOf(this)
    }

    fun readResolve(): Any {
        events = LanguageModelEvents()
        return this
    }

    override fun toString(): String = buildString {
        appendLine("Name: $displayName (LFM2.5-230M)")
        appendLine("Weights: ${weightsDirectory.ifEmpty { "not set" }}${if (isLoaded) "" else " (not loaded)"}")
        append("Text: ${text.take(120)}")
    }

    class CreationTemplate : EditableObject {

        @UserParameter(
            label = "Max sequence length",
            description = "KV cache capacity and the display window; generation stops when it fills",
            order = 1,
        )
        var maxSeqLen = 512

        fun create(weightsDirectory: String): LanguageModel =
            LanguageModel(weightsDirectory, maxSeqLen)

        override val name = "Language Model"
    }
}
