package org.simbrain.network.llm

import org.simbrain.network.compositor.AttentionTile
import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.Lfm2StackCompositor
import org.simbrain.network.core.Network
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.tensor.Blas
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.HuggingFaceFileTokenizer
import org.simbrain.util.Tokenizer
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
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
 * The coupling vocabulary and document-sync protocol live on [GenerativeModel]; everything
 * needing the tokenizer or unembedding stays on this side of the coupling. All attributes are
 * safe to call while weights are unloaded (empty results, no-op consumption).
 */
class LanguageModel @XStreamConstructor constructor() : GenerativeModel() {

    /** The model's real tokenization, for consumers drawing token boundaries; path-derived. */
    override val displayTokenizer: Tokenizer<*>
        get() = HuggingFaceFileTokenizer(
            Path.of(weightsDirectory).resolve("tokenizer.json").toString()
        )

    /** Directory containing `model.safetensors` and `tokenizer.json`. */
    var weightsDirectory: String = ""

    var maxSeqLen: Int = 512
        private set

    override var prompt by GuiEditable(
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

    override var samplingStrategy: SamplingStrategy by GuiEditable(
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

    var enableDemoTools by GuiEditable(
        initValue = false,
        label = "Enable demo tools",
        description = "Advertise the built-in offline demo tools (current time, canned weather) " +
            "in chat mode and answer the model's calls to them",
        order = 8,
    )

    /** Tools registered by simulations, advertised alongside the demo tools in chat mode. */
    @Transient
    var tools: List<LlmTool> = emptyList()

    private fun activeTools(): List<LlmTool> =
        if (enableDemoTools) tools + demoTools else tools

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

    @Transient
    override var events: LanguageModelEvents = LanguageModelEvents()
        private set

    class LoadedState(val model: Lfm2Model, val tokenizer: LlmTokenizer, var scene: CompositorScene)

    @Transient
    var loaded: LoadedState? = null
        private set

    val isLoaded get() = loaded != null

    @Transient
    private var generatedCount = 0

    /** The committed token stream in [text] order: prompt, injections, accepted samples. */
    @Transient
    private var windowIds = ArrayList<Int>()

    /** Sampled ids between the tool-call markers; null while not capturing a call. */
    @Transient
    private var toolCallBuffer: MutableList<Int>? = null

    /** Calls parsed from a completed capture, executed when the assistant turn closes. */
    @Transient
    private var pendingToolCalls: List<Lfm2ChatFormat.ToolCall>? = null

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

    /** Resets the model, encodes a fresh run from [prompt], and clears the tool-loop state. */
    override fun onRestart(): IntArray? {
        val state = loaded ?: return null
        state.model.reset()
        state.scene.reset()
        val ids = encodePrompt(state.tokenizer)
        windowIds = ArrayList(ids.toList())
        generatedCount = 0
        toolCallBuffer = null
        pendingToolCalls = null
        text = prompt
        return ids
    }

    override fun hasRunToContinue(): Boolean = pending.isNotEmpty() || sampledToken >= 0

    /** A full context window cannot be resumed; [startGeneration] resets it. */
    override fun canResume(): Boolean =
        loaded?.let { it.model.position < it.model.config.maxSeqLen } == true

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
            trackToolCall(state, sampledToken)
            val stopAtEos = stopAtEndOfText || promptMode == PromptMode.CHAT
            if (stopAtEos && sampledToken == state.model.config.eosTokenId) {
                val calls = pendingToolCalls
                if (calls != null) {
                    pendingToolCalls = null
                    answerToolCalls(state, calls)
                } else {
                    isGenerating = false
                }
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

    /** The residual stream at [selectedLayer] for the last processed token. */
    override fun computeHiddenState(): DoubleArray {
        val state = loaded ?: return DoubleArray(0)
        val layer = selectedLayer.coerceIn(0, state.model.config.numLayers - 1)
        val tensor = state.model.plan.port("layers.$layer.resid").tensor
        return DoubleArray(tensor.size) { tensor.data.get(it).toDouble() }
    }

    override fun hiddenStateLabel() = "layer $selectedLayer residual"

    /** Injection and window edits encode without specials, so scaffolding never doubles. */
    override fun encodeText(textIn: String): IntArray? =
        loaded?.tokenizer?.encode(textIn, addSpecials = false)

    override fun onInjected(newText: String, ids: IntArray) {
        ids.forEach { windowIds.add(it) }
        text += newText
    }

    /** The full committed token stream, decoded with specials kept — scaffolding included. */
    override fun windowText(): String? =
        loaded?.let { it.tokenizer.decode(windowIds.toIntArray()) }

    /**
     * An edit resets the model and requeues the whole edited window for a watchable re-prefill —
     * the conv caches make an exact prefix rewind impossible.
     */
    override fun applyWindowEdit(ids: IntArray) {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        pending = ArrayDeque(ids.toList())
        windowIds = ArrayList(ids.toList())
        generatedCount = 0
        toolCallBuffer = null
        pendingToolCalls = null
        text = state.tokenizer.decode(ids, skipSpecials = true)
    }

    private fun trackToolCall(state: LoadedState, id: Int) {
        val buffer = toolCallBuffer
        when {
            id == Lfm2ChatFormat.TOOL_CALL_START_ID -> toolCallBuffer = ArrayList()
            buffer == null -> {}
            id == Lfm2ChatFormat.TOOL_CALL_END_ID -> {
                toolCallBuffer = null
                pendingToolCalls = Lfm2ChatFormat
                    .parseToolCalls(state.tokenizer.decode(buffer.toIntArray()))
                    .takeIf { it.isNotEmpty() }
            }
            else -> buffer.add(id)
        }
    }

    /**
     * Executes [calls] and injects the tool-result turn into the feed queue — the closing
     * `im_end` is fed first, then the tool turn and a reopened assistant turn — so decoding
     * continues through the answer instead of stopping.
     */
    private fun answerToolCalls(state: LoadedState, calls: List<Lfm2ChatFormat.ToolCall>) {
        val results = calls.map { call ->
            val tool = activeTools().firstOrNull { it.name == call.name }
            if (tool == null) "error: unknown tool ${call.name}"
            else runCatching { tool.execute(call.arguments) }.getOrElse { "error: ${it.message}" }
        }
        val ids = state.tokenizer.encode(
            Lfm2ChatFormat.toolResultTurn(results.joinToString("\n")),
            addSpecials = false,
        )
        if (pending.isEmpty() && sampledToken >= 0) pending.addLast(sampledToken)
        ids.forEach {
            pending.addLast(it)
            windowIds.add(it)
        }
        syncGate.invalidate()
        text += state.tokenizer.decode(ids, skipSpecials = true)
    }

    /**
     * Encodes [prompt] for a fresh run. Chat mode builds the full templated string — BOS text
     * included — and encodes with specials off, so the post-processor cannot add a second BOS;
     * available tools are advertised as plain text in the system turn, per the template.
     */
    private fun encodePrompt(tokenizer: LlmTokenizer): IntArray = when (promptMode) {
        PromptMode.COMPLETION -> tokenizer.encode(prompt)
        PromptMode.CHAT -> {
            val toolLine = activeTools().takeIf { it.isNotEmpty() }?.let(Lfm2ChatFormat::toolListLine)
            val system = listOfNotNull(systemPrompt.takeIf { it.isNotEmpty() }, toolLine)
                .joinToString("\n")
            tokenizer.encode(Lfm2ChatFormat.chatPrompt(prompt, system), addSpecials = false)
        }
    }

    private fun sampleToken(logits: FloatTensor): Int {
        sampleOverride?.let { return it() }
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

    fun readResolve(): Any {
        events = LanguageModelEvents()
        return this
    }

    override fun toString(): String = buildString {
        appendLine("Name: $displayName (LFM2.5-230M)")
        appendLine("Weights: ${weightsDirectory.ifEmpty { "not set" }}${if (isLoaded) "" else " (not loaded)"}")
        append("Text: ${text.take(120)}")
    }

    companion object {

        /** Offline built-in tools for the tool-calling demo; [enableDemoTools] advertises them. */
        val demoTools: List<LlmTool> = listOf(
            LlmTool(
                name = "current_time",
                description = "Get the current date and time",
                parameters = """{"type": "object", "properties": {}, "required": []}""",
            ) {
                java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy, h:mm a"))
            },
            LlmTool(
                name = "get_weather",
                description = "Get the current weather at a location",
                parameters = """{"type": "object", "properties": {"location": """ +
                    """{"type": "string", "description": "City name"}}, "required": ["location"]}""",
            ) { arguments ->
                "Sunny, 22 degrees C in ${arguments["location"] ?: "your location"} (demo data)"
            },
        )
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
