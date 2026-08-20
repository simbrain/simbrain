package org.simbrain.network.llm

import org.simbrain.network.compositor.*
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkDebugInfoProvider
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
import org.simbrain.util.roundToString
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
class LanguageModel @XStreamConstructor constructor() : GenerativeModel(), NetworkDebugInfoProvider {

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

    var tokensToGenerate by GuiEditable(
        initValue = 0,
        label = "Tokens to generate",
        description = "Generation stops after this many tokens beyond the fed context; " +
            "0 generates until the context window fills",
        min = 0,
        order = 4,
    )

    var temperature by GuiEditable(
        initValue = 0.1,
        label = "Temperature",
        description = "Softmax temperature applied to the logits before sampling",
        min = 0.01,
        max = 4.0,
        increment = 0.05,
        order = 5,
    )

    var probabilityCardCandidates by GuiEditable(
        initValue = 8,
        label = "Probability card candidates",
        description = "Number of highest-probability next tokens shown in the canvas card",
        min = 1,
        max = 20,
        order = 5,
    )

    override var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.TopK(50),
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

    /** Whether the local demonstration tools are advertised when a chat session begins. */
    @Transient
    var demoToolsEnabled = false

    /** Tools registered by simulations, advertised alongside enabled built-in tools in chat mode. */
    @Transient
    var tools: List<LlmTool> = emptyList()

    private fun activeTools(): List<LlmTool> =
        tools + if (demoToolsEnabled) demoTools else emptyList()

    /** The model layer the structure view shows; default to the first attention/KV-cache layer. */
    var selectedLayer: Int = 2

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
    var hideInactiveLimb: Boolean = true
        set(value) {
            field = value
            loaded?.scene?.hideDimmed = value
        }

    /** Whether the structure view shows depth-card fans behind stacked layer tiles. */
    var showLayerCards: Boolean = false
        set(value) {
            field = value
            loaded?.scene?.showLayerCards = value
        }

    /** Saved tile positions by tile id, applied to the scene on load. */
    var tileLayout: HashMap<String, DoubleArray>? = null

    /** Saved position of the next-token probability card. */
    var probabilityCardLayout: DoubleArray? = null

    @Transient
    override var events: LanguageModelEvents = LanguageModelEvents()
        private set

    class LoadedState(val model: Lfm2Model, val tokenizer: LlmTokenizer, var scene: CompositorScene)

    @Transient
    var loaded: LoadedState? = null
        private set

    val isLoaded get() = loaded != null

    /** Tokens generated since the last edit or injection; drives the [tokensToGenerate] budget. */
    @Transient
    var generatedCount = 0
        private set

    /** Tokens the model has processed from the current window. */
    val fedTokenCount: Int
        get() = loaded?.model?.position ?: 0

    /** Tokens committed to the window, fed or still queued. */
    val windowTokenCount: Int
        get() = windowIds.size

    @Transient
    @Volatile
    var tokenProbabilitySnapshot: TokenProbabilitySnapshot? = null
        private set

    /** The committed token stream in [text] order: seed, injections, accepted samples. */
    @Transient
    private var windowIds = ArrayList<Int>()

    private class ConvCheckpoint(val position: Int, val convState: List<FloatArray>)

    /** Periodic conv-cache snapshots, so an edit rewinds to a nearby position instead of zero. */
    @Transient
    private var checkpoints = ArrayList<ConvCheckpoint>()

    /** Bounds checkpoint memory to ~16 snapshots regardless of the context window size. */
    private val checkpointInterval: Int
        get() = maxOf(32, maxSeqLen / 16)

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
    @Transient
    private var loadedFromDirectory: String? = null

    @Synchronized
    fun loadWeights() {
        // A repeat load from the same directory (e.g. a second node created by undo while the
        // first load ran) would rebuild ~1 GB of state and desync the window; skip it.
        if (loaded != null && loadedFromDirectory == weightsDirectory) return
        val dir = Path.of(weightsDirectory)
        check(Lfm2Weights.isValidWeightsDirectory(dir)) {
            "No model.safetensors and tokenizer.json in $weightsDirectory"
        }
        Blas.numThreads = 4
        val model = Lfm2Model(Lfm2Config(maxSeqLen = maxSeqLen), Safetensors.load(dir.resolve("model.safetensors")))
        val tokenizer = LlmTokenizer(dir.resolve("tokenizer.json"))
        loaded = LoadedState(model, tokenizer, buildScene(model))
        loadedFromDirectory = weightsDirectory
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
        val attention = scene.tiles.firstOrNull { it.id == "block.attn.weights" } as? AttentionTile
        attention?.selectedHead = selectedHead
        // Chain onto the compositor's GQA coupling so a pager flip lands in the serialized head,
        // and replay the restored head through it so the k/v cache decks start on the right group.
        val headSelect = scene.onHeadSelected
        scene.onHeadSelected = { tile, head ->
            headSelect?.invoke(tile, head)
            if (tile is AttentionTile) selectedHead = head
        }
        attention?.let { scene.onHeadSelected?.invoke(it, selectedHead) }
        scene.historyView = historyView
        scene.hideDimmed = hideInactiveLimb
        scene.showLayerCards = showLayerCards
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
        // Install the async runner only after the synchronous initial select above, so the
        // scene arrives fully derived. Replay work then runs off the EDT under the model
        // monitor, mutually exclusive with step() and rewinds.
        val replayWorker = LatestWinsRunner(LatestWinsRunner.sharedWorker) {
            scene.replayPending = false
            scene.stackStateDirty = true
            events.updateGraphics.fire()
        }
        scene.replayRunner = { block ->
            scene.replayPending = true
            replayWorker.submit { synchronized(this) { block() } }
        }
        return scene
    }

    /** Copies the scene's current tile positions into [tileLayout] so they serialize. */
    fun captureViewState() {
        val scene = loaded?.scene ?: return
        tileLayout = scene.tiles.associateTo(HashMap()) { it.id to doubleArrayOf(it.x, it.y) }
    }

    override fun appendNetworkDebugInfo(builder: StringBuilder, indent: String) {
        builder.appendLine("${indent}LFM2.5-230M: loaded=$isLoaded, context=$maxSeqLen, " +
            "selected layer=$selectedLayer, selected head=$selectedHead, history=$historyView")
        val scene = loaded?.scene
        if (scene == null) {
            builder.appendLine("${indent}Interior unavailable until weights are loaded.")
            return
        }
        builder.appendLine("${indent}Interior tiles (${scene.tiles.size}):")
        scene.tiles.forEach { tile ->
            val layers = (tile as? LayerStacked)?.stackLayers?.joinToString(prefix = " layers=") ?: ""
            builder.appendLine("${indent}  [${tile::class.simpleName}] ${tile.id} (${tile.title})$layers  " +
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
     * A copy carries the recipe, not the runtime: the weights path, settings, and view state,
     * plus the committed window as seed text. The copy starts unloaded and loads like a fresh
     * model; the seed replays through prefill, so it reaches the original's window state by
     * recomputation rather than by copying caches.
     */
    @Synchronized
    fun copy(): LanguageModel = LanguageModel(weightsDirectory, maxSeqLen).also { copy ->
        copy.label = label
        copy.location = location
        copy.promptMode = promptMode
        copy.tokensToGenerate = tokensToGenerate
        copy.temperature = temperature
        copy.probabilityCardCandidates = probabilityCardCandidates
        copy.samplingStrategy = samplingStrategy.copy() as SamplingStrategy
        copy.stopAtEndOfText = stopAtEndOfText
        copy.pauseWorkspaceAtEnd = pauseWorkspaceAtEnd
        copy.tools = tools
        copy.selectedLayer = selectedLayer
        copy.selectedHead = selectedHead
        copy.lensEnabled = lensEnabled
        copy.historyView = historyView
        copy.hideInactiveLimb = hideInactiveLimb
        copy.showLayerCards = showLayerCards
        copy.tileLayout = tileLayout?.mapValuesTo(HashMap()) { it.value.copyOf() }
        copy.probabilityCardLayout = probabilityCardLayout?.copyOf()
        copy.initialText = windowText()?.takeIf { it.isNotBlank() } ?: initialText
    }

    /**
     * Fills a fresh window with [textIn], queued for a watchable prefill. Used once at first
     * load and available to harnesses; the document owns the window afterwards. Encoding
     * leaves specials alone and guards the leading BOS, so a seed that is already a full
     * window — a copied model's captured stream — round-trips without doubling the marker.
     */
    @Synchronized
    fun seedWindow(textIn: String) {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        val raw = state.tokenizer.encode(textIn, addSpecials = false)
        val ids = if (raw.firstOrNull() == Lfm2ChatFormat.BOS_ID) raw.toList()
            else listOf(Lfm2ChatFormat.BOS_ID) + raw.toList()
        windowIds = ArrayList(ids)
        pending = ArrayDeque(ids)
        checkpoints = ArrayList()
        sampledToken = -1
        lastGenerated = ""
        currentSpan = IntArray(0)
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
        checkpoints = ArrayList()
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
        val fromQueue = pending.isNotEmpty()
        val id = if (fromQueue) pending.removeFirst() else sampledToken
        val position = state.model.position
        val logits = state.model.forwardToken(id)
        if (state.model.position % checkpointInterval == 0) {
            checkpoints.add(ConvCheckpoint(state.model.position, state.model.snapshotConvState()))
        }
        sampledToken = sampleToken(logits)
        tokenProbabilitySnapshot = TokenProbabilitySnapshot.topK(
            logits, probabilityCardCandidates, sampledToken, temperature,
        )
        state.scene.publish(position)
        var spanIndex = if (fromQueue) position else -1
        if (pending.isEmpty()) {
            windowIds.add(sampledToken)
            if (!fromQueue) spanIndex = windowIds.lastIndex
            syncGate.invalidate()
            trackToolCall(state, sampledToken)
            if (sealsAtEndOfText && sampledToken == state.model.config.eosTokenId) {
                pendingToolCalls?.let { calls ->
                    pendingToolCalls = null
                    answerToolCalls(state, calls)
                }
            } else {
                // Delta of successive whole-window decodes, not a single-token decode: a
                // byte-fallback token holding part of a multi-byte character decodes alone to
                // replacement characters. The delta emits nothing while a character is
                // incomplete and the whole character once its last byte lands.
                val skipSpecials = promptMode == PromptMode.CHAT
                val ids = windowIds.toIntArray()
                val before = state.tokenizer.decode(ids.copyOf(ids.size - 1), skipSpecials)
                    .withoutIncompleteTail()
                val after = state.tokenizer.decode(ids, skipSpecials).withoutIncompleteTail()
                lastGenerated = if (after.length > before.length) after.substring(before.length) else ""
                text += lastGenerated
                generatedCount++
            }
        }
        currentSpan = tokenSpan(spanIndex)
        events.updated.fire()
    }

    /**
     * Char range of the [index]th window token in [windowText]'s coordinates, computed from
     * prefix decode lengths so special tokens and byte-level merges land exactly where the
     * synced document renders them. Prefix decodes are trimmed like [windowText], keeping the
     * coordinates aligned: a token ending mid-character gets an empty span (the character is
     * not rendered yet), and the token completing it spans the whole character.
     */
    private fun tokenSpan(index: Int): IntArray {
        val state = loaded ?: return IntArray(0)
        if (index < 0 || index >= windowIds.size) return IntArray(0)
        val start = if (index == 0) 0
            else state.tokenizer.decode(windowIds.subList(0, index).toIntArray())
                .withoutIncompleteTail().length
        val end = state.tokenizer.decode(windowIds.subList(0, index + 1).toIntArray())
            .withoutIncompleteTail().length
        return if (end > start) intArrayOf(start, end) else IntArray(0)
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
        // Every canonical window starts with BOS; the first injection restores it to both the
        // committed stream and the feed queue, mirroring seedWindow and applyWindowEdit.
        if (windowIds.isEmpty() && ids.firstOrNull() != Lfm2ChatFormat.BOS_ID) {
            windowIds.add(Lfm2ChatFormat.BOS_ID)
            pending.addFirst(Lfm2ChatFormat.BOS_ID)
        }
        ids.forEach { windowIds.add(it) }
        text += newText
        generatedCount = 0
    }

    /**
     * The full committed token stream, decoded with specials kept — scaffolding included. A
     * trailing incomplete multi-byte character (byte-fallback tokens still waiting for their
     * remaining bytes) decodes to a replacement character; it is withheld until the character
     * completes, so the synced document never shows a half-streamed character. Trimming here
     * keeps publish and edit-detection consistent: both sides of [contextWindow] compare
     * against the same text.
     */
    override fun windowText(): String? =
        loaded?.let { it.tokenizer.decode(windowIds.toIntArray()).withoutIncompleteTail() }

    /**
     * An edit rebuilds the context and requeues a watchable re-prefill, reusing the cached
     * prefix where it can: the attention KV caches rewind by position alone, and the conv
     * caches — the one destructively rolled state — restore from the nearest periodic
     * checkpoint at or before the edit point, so only the tokens from that checkpoint on
     * replay instead of the whole window. With no usable checkpoint the model resets and
     * replays from the top, exactly as before. A window whose leading BOS marker was edited
     * away gets it restored: every canonical window starts with BOS, and without it the model
     * tends to end the text immediately. The next sync republishes the marker.
     */
    override fun applyWindowEdit(ids: IntArray) {
        val state = loaded ?: return
        val window = if (ids.firstOrNull() == Lfm2ChatFormat.BOS_ID) ids.toList()
            else listOf(Lfm2ChatFormat.BOS_ID) + ids.toList()
        val reusable = reusablePrefixLength(window)
        // A checkpoint at the window's full length would leave nothing to feed; the last
        // replayed token's forward pass is what samples the continuation.
        val checkpoint = checkpoints.lastOrNull { it.position <= reusable && it.position < window.size }
        when {
            // Pure append with the caches already exactly at the reuse point: nothing to
            // rewind or truncate, just feed the new suffix.
            reusable == state.model.position && reusable < window.size -> {
                pending = ArrayDeque(window.subList(reusable, window.size))
            }
            checkpoint == null -> {
                state.model.reset()
                state.scene.reset()
                checkpoints = ArrayList()
                pending = ArrayDeque(window)
            }
            else -> {
                state.model.rewindTo(checkpoint.position, checkpoint.convState)
                state.scene.truncateFrom(checkpoint.position)
                while (checkpoints.isNotEmpty() && checkpoints.last().position > checkpoint.position) {
                    checkpoints.removeAt(checkpoints.size - 1)
                }
                pending = ArrayDeque(window.subList(checkpoint.position, window.size))
            }
        }
        windowIds = ArrayList(window)
        currentSpan = IntArray(0)
        generatedCount = 0
        toolCallBuffer = null
        pendingToolCalls = null
        text = state.tokenizer.decode(ids, skipSpecials = true)
    }

    /**
     * How many leading tokens of the edited [window] the model's caches still cover: the first
     * divergence from the committed stream, bounded by what has actually been fed. Diffed
     * against the BOS-restored window, since [windowIds] always carries the leading marker.
     */
    private fun reusablePrefixLength(window: List<Int>): Int {
        val fed = minOf(loaded?.model?.position ?: 0, windowIds.size, window.size)
        var i = 0
        while (i < fed && windowIds[i] == window[i]) i++
        return i
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
     * chat scaffolding (BOS and, when enabled, the tool list) is
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
     * The conversation opening contains literal BOS text (encoded with specials off, so the
     * post-processor cannot add a second BOS) and, when tools are active, a system turn
     * advertising them.
     */
    private fun chatScaffolding(): String {
        val toolLine = activeTools().takeIf { it.isNotEmpty() }?.let(Lfm2ChatFormat::toolListLine)
        val system = toolLine.orEmpty()
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
        // An uncoupled model reopens with text but no window; reseeding from the saved text
        // restores it. Completion mode only: the chat transcript strips its turn scaffolding,
        // so re-encoding it would build a structurally wrong window. A coupled document
        // replays over the seed as an ordinary edit either way.
        if (promptMode == PromptMode.COMPLETION && text.isNotBlank()) initialText = text
        return this
    }

    override fun toString(): String = buildString {
        appendLine("Name: $displayName (LFM2.5-230M)")
        appendLine("Weights: ${weightsDirectory.ifEmpty { "not set" }}${if (isLoaded) "" else " (not loaded)"}")
        append("Text: ${text.take(120)}")
    }

    companion object {

        /** Offline built-in tools for the tool-calling demonstration. */
        val demoTools: List<LlmTool> = listOf(
            LlmTool(
                name = "current_time",
                description = "Get the current date and time",
                parameters = """{"type": "object", "properties": {}, "required": []}""",
            ) {
                java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy, h:mm a"))
            },
        )
    }

    class CreationTemplate : EditableObject {

        @UserParameter(
            label = "Context window size",
            description = "Maximum number of tokens the model can retain (up to 8,192). " +
                "Larger windows use substantially more memory; 512 is recommended for most computers.",
            minimumValue = 1.0,
            maximumValue = 8192.0,
            order = 1,
        )
        var contextWindowSize = 512

        fun create(weightsDirectory: String): LanguageModel =
            LanguageModel(weightsDirectory, contextWindowSize)

        override val name = "Language Model"
    }
}

/**
 * Drops the trailing replacement-character run an incomplete multi-byte character decodes to
 * (the tokenizer's lossy UTF-8 collapses a truncated trailing sequence to one U+FFFD).
 * Completed characters never decode to replacement characters, so only an in-flight tail is
 * affected; a genuine trailing U+FFFD in the stream is hidden until any character follows it.
 */
private fun String.withoutIncompleteTail() = trimEnd('\uFFFD')
