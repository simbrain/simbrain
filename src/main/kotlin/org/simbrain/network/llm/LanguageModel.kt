package org.simbrain.network.llm

import org.simbrain.network.compositor.AttentionTile
import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.HistoryView
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
import org.simbrain.workspace.Consumable
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
 * button steps generation token by token. There is no run mode and no prompt: an optional
 * [initialText] seeds the first window at load, the coupled document owns the context from
 * then on, and the model advances until the stream seals at end-of-text, the context window
 * fills, or an optional token budget is spent — after which edits, injections, or a sent
 * chat message move it again.
 *
 * Only the weights directory, config, sampling parameters, and view state are serialized —
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

    /** One-shot text the first load seeds the window with; the document owns it afterwards. */
    @Transient
    var initialText: String? = null

    var promptMode: PromptMode by GuiEditable(
        initValue = PromptMode.COMPLETION,
        label = "Prompt mode",
        description = "Completion continues the document verbatim; " +
            "chat treats it as a conversation the model answers in",
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
        description = "Generation stops after this many tokens beyond the fed context; " +
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

    var pauseWorkspaceAtEnd by GuiEditable(
        initValue = true,
        label = "Pause workspace when the run ends",
        description = "Stop the workspace once generation halts — end of text, full window, or " +
            "spent token budget — so a coupled document unlocks for editing",
        order = 8,
    )

    var enableDemoTools by GuiEditable(
        initValue = false,
        label = "Enable demo tools",
        description = "Advertise the built-in offline demo tools (current time, canned weather) " +
            "in chat mode and answer the model's calls to them",
        order = 9,
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

    /** How the scene treats token history: recorded, ghosted to the live row, or not kept. */
    var historyView: HistoryView = HistoryView.FULL
        set(value) {
            field = value
            loaded?.scene?.historyView = value
        }

    /** When true, the limb the selected layer doesn't use is hidden instead of ghosted. */
    var hideInactiveLimb: Boolean = false
        set(value) {
            field = value
            loaded?.scene?.hideDimmed = value
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

    /** The committed token stream in [text] order: seed, injections, accepted samples. */
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
        initialText?.takeIf { it.isNotBlank() }?.let { seedWindow(it) }
        initialText = null
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
        scene.historyView = historyView
        scene.hideDimmed = hideInactiveLimb
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

    /**
     * Fills a fresh window with [textIn], queued for a watchable prefill. Used once at first
     * load and available to harnesses; the document owns the window afterwards.
     */
    @Synchronized
    fun seedWindow(textIn: String) {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        val ids = state.tokenizer.encode(textIn)
        windowIds = ArrayList(ids.toList())
        pending = ArrayDeque(ids.toList())
        sampledToken = -1
        lastGenerated = ""
        generatedCount = 0
        toolCallBuffer = null
        pendingToolCalls = null
        text = textIn
        syncGate.reset()
        events.updated.fire()
    }

    override fun onClear() {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        windowIds = ArrayList()
        generatedCount = 0
        toolCallBuffer = null
        pendingToolCalls = null
    }

    override fun hasContinuation(): Boolean = sampledToken >= 0

    private val sealsAtEndOfText get() = stopAtEndOfText || promptMode == PromptMode.CHAT

    /**
     * The stream ends where the model ended it: a window whose last token is end-of-text is
     * sealed until an edit, an injection, or a tool answer moves it past the marker.
     */
    val isSealed: Boolean
        get() = sealsAtEndOfText && pending.isEmpty() &&
            windowIds.lastOrNull() == loaded?.model?.config?.eosTokenId

    val isWindowFull: Boolean
        get() = loaded?.let { it.model.position >= it.model.config.maxSeqLen } == true

    /** [tokensToGenerate] spent; edits and injections start a fresh budget. */
    val budgetSpent: Boolean
        get() = tokensToGenerate > 0 && generatedCount >= tokensToGenerate

    override fun isHalted(): Boolean = loaded == null || isSealed || isWindowFull || budgetSpent

    context(Network)
    override fun update() {
        step()
    }

    /**
     * Advances generation by one token: feeds the next pending token (or the last sampled token
     * once the queue is drained), publishes activations to the scene, and samples the next
     * token. No-op unless [canAdvance].
     */
    @Synchronized
    fun step() {
        lastGenerated = ""
        val state = loaded ?: return
        if (!canAdvance) return
        val id = if (pending.isNotEmpty()) pending.removeFirst() else sampledToken
        val position = state.model.position
        val logits = state.model.forwardToken(id)
        state.scene.publish(position)
        sampledToken = sampleToken(logits)
        if (pending.isEmpty()) {
            windowIds.add(sampledToken)
            syncGate.invalidate()
            trackToolCall(state, sampledToken)
            if (sealsAtEndOfText && sampledToken == state.model.config.eosTokenId) {
                pendingToolCalls?.let { calls ->
                    pendingToolCalls = null
                    answerToolCalls(state, calls)
                }
            } else {
                lastGenerated = state.tokenizer.decode(
                    intArrayOf(sampledToken),
                    skipSpecials = promptMode == PromptMode.CHAT,
                )
                text += lastGenerated
                generatedCount++
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
        generatedCount = 0
    }

    /** The full committed token stream, decoded with specials kept — scaffolding included. */
    override fun windowText(): String? =
        loaded?.let { it.tokenizer.decode(windowIds.toIntArray()) }

    /**
     * An edit resets the model and requeues the whole edited window for a watchable re-prefill —
     * the conv caches make an exact prefix rewind impossible. A window whose leading BOS marker
     * was edited away gets it restored: every canonical window starts with BOS, and without it
     * the model tends to end the text immediately. The next sync republishes the marker.
     */
    override fun applyWindowEdit(ids: IntArray) {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        val window = if (ids.firstOrNull() == Lfm2ChatFormat.BOS_ID) ids.toList()
            else listOf(Lfm2ChatFormat.BOS_ID) + ids.toList()
        pending = ArrayDeque(window)
        windowIds = ArrayList(window)
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
     * Appends a templated user turn and the open assistant turn to the stream — the chat
     * counterpart of [injectText], with the template applied on this side of the coupling,
     * like a chat runtime would. On an empty window the message starts the conversation: the
     * chat scaffolding (BOS, then a system turn carrying [systemPrompt] and the tool list) is
     * prepended, so the first send produces exactly the canonical chat prompt. Sending into a
     * sealed stream moves it past the end marker (the unfed marker is queued first, the same
     * way a tool answer reopens the turn). Meant for chat mode.
     */
    @Synchronized
    @Consumable
    fun sendUserMessage(message: String) {
        if (message.isBlank()) return
        val state = loaded ?: return
        val prefix = if (windowIds.isEmpty()) chatScaffolding() else ""
        val ids = state.tokenizer.encode(
            prefix + Lfm2ChatFormat.turn("user", message.trim()) + Lfm2ChatFormat.GENERATION_PROMPT,
            addSpecials = false,
        )
        if (pending.isEmpty() && sampledToken >= 0) pending.addLast(sampledToken)
        ids.forEach {
            pending.addLast(it)
            windowIds.add(it)
        }
        generatedCount = 0
        syncGate.invalidate()
        text += state.tokenizer.decode(ids, skipSpecials = true)
        events.updated.fire()
    }

    /**
     * The conversation opening a first message sits on: literal BOS text (encoded with
     * specials off, so the post-processor cannot add a second BOS), then a system turn
     * carrying [systemPrompt] and the plain-text tool advertisement, per the template.
     */
    private fun chatScaffolding(): String {
        val toolLine = activeTools().takeIf { it.isNotEmpty() }?.let(Lfm2ChatFormat::toolListLine)
        val system = listOfNotNull(systemPrompt.takeIf { it.isNotEmpty() }, toolLine)
            .joinToString("\n")
        return Lfm2ChatFormat.BOS +
            if (system.isEmpty()) "" else Lfm2ChatFormat.turn("system", system)
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
        // Saves from before the field existed deserialize it to null despite the non-null type.
        @Suppress("SENSELESS_COMPARISON")
        if (historyView == null) historyView = HistoryView.FULL
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
            label = "Starting text",
            description = "Seeds the first context window; the coupled document owns the window afterwards",
            order = 1,
        )
        var startingText = "The capital of France is"

        @UserParameter(
            label = "Max sequence length",
            description = "KV cache capacity and the display window; generation stops when it fills",
            order = 2,
        )
        var maxSeqLen = 512

        fun create(weightsDirectory: String): LanguageModel =
            LanguageModel(weightsDirectory, maxSeqLen).also { it.initialText = startingText }

        override val name = "Language Model"
    }
}
