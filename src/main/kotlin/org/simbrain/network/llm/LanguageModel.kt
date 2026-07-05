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
import java.awt.geom.Point2D
import java.nio.file.Path
import kotlin.math.exp

class LanguageModelEvents : LocationEvents() {
    val weightsLoaded = NoArgEvent()
}

/**
 * A language model on the network canvas: wraps the headless [Lfm2Model] and exposes its interior
 * through a [CompositorScene]. One network update generates one token, so the workspace play
 * button steps generation token by token.
 *
 * Only the weights directory, config, prompt, sampling parameters, and view state are serialized —
 * never the weights. On deserialization the model reloads from [weightsDirectory]; if the files
 * are missing the model stays unloaded and the GUI offers to relocate or download them.
 */
class LanguageModel @XStreamConstructor constructor() : LocatableModel(), EditableObject {

    /** Directory containing `model.safetensors` and `tokenizer.json`. */
    var weightsDirectory: String = ""

    var maxSeqLen: Int = 512
        private set

    var displaySeq: Int = 128
        private set

    var prompt by GuiEditable(
        initValue = "The capital of France is",
        label = "Prompt",
        description = "Text the model continues from. Generation restarts from this prompt.",
        order = 1,
    )

    var tokensToGenerate by GuiEditable(
        initValue = 30,
        label = "Tokens to generate",
        description = "Generation stops after this many tokens beyond the prompt",
        min = 1,
        order = 2,
    )

    var temperature by GuiEditable(
        initValue = 1.0,
        label = "Temperature",
        description = "Softmax temperature applied to the logits before sampling",
        min = 0.01,
        max = 4.0,
        increment = 0.05,
        order = 3,
    )

    var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.Greedy,
        label = "Sampling strategy",
        description = "How the next token is chosen from the distribution",
        showDetails = false,
        order = 4,
    )

    var stopAtEndOfText by GuiEditable(
        initValue = true,
        label = "Stop at end of text",
        description = "End the run when the model emits its end-of-text token",
        order = 5,
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

    @Transient
    private var promptIds = IntArray(0)

    @Transient
    private var cursor = 0

    @Transient
    private var generatedCount = 0

    @Transient
    private var sampledToken = -1

    @Transient
    var isGenerating = false
        private set

    private val attentionTile
        get() = loaded?.scene?.tiles?.firstOrNull { it.id == "block.attn.weights" } as? AttentionTile

    constructor(weightsDirectory: String, maxSeqLen: Int = 512, displaySeq: Int = 128) : this() {
        this.weightsDirectory = weightsDirectory
        this.maxSeqLen = maxSeqLen
        this.displaySeq = displaySeq
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
        events.weightsLoaded.fire()
    }

    private fun buildScene(model: Lfm2Model): CompositorScene {
        val scene = Lfm2StackCompositor.buildScene(model, displaySeq)
        tileLayout?.forEach { (id, xy) ->
            scene.tiles.firstOrNull { it.id == id }?.let {
                it.x = xy[0]
                it.y = xy[1]
            }
        }
        (scene.tiles.firstOrNull { it.id == "block.attn.weights" } as? AttentionTile)
            ?.selectedHead = selectedHead
        scene.lens?.enabled = lensEnabled
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

    /** Resets the model and starts a fresh generation run from [prompt]. */
    @Synchronized
    fun startGeneration() {
        val state = loaded ?: return
        state.model.reset()
        state.scene.reset()
        promptIds = state.tokenizer.encode(prompt)
        cursor = 0
        generatedCount = 0
        sampledToken = -1
        text = prompt
        isGenerating = true
        events.updated.fire()
    }

    @Synchronized
    fun stopGeneration() {
        isGenerating = false
    }

    context(Network)
    override fun update() {
        step()
    }

    /**
     * Advances generation by one token: feeds the next prompt token (or the last sampled token
     * once the prompt is consumed), publishes activations to the scene, and samples the next
     * token. No-op unless a run is active.
     */
    @Synchronized
    fun step() {
        val state = loaded ?: return
        if (!isGenerating) return
        if (state.model.position >= state.model.config.maxSeqLen) {
            isGenerating = false
            return
        }
        val id = if (cursor < promptIds.size) promptIds[cursor] else sampledToken
        val position = state.model.position
        val logits = state.model.forwardToken(id)
        state.scene.publish(position)
        sampledToken = sampleToken(logits)
        if (cursor >= promptIds.size - 1) {
            if (stopAtEndOfText && sampledToken == state.model.config.eosTokenId) {
                isGenerating = false
            } else {
                text += state.tokenizer.decode(intArrayOf(sampledToken))
                generatedCount++
                if (generatedCount >= tokensToGenerate) {
                    isGenerating = false
                }
            }
        }
        cursor++
        events.updated.fire()
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
            description = "KV cache capacity; generation stops when it fills",
            order = 1,
        )
        var maxSeqLen = 512

        @UserParameter(
            label = "Displayed tokens",
            description = "Token rows retained in the heatmaps; generation beyond this still runs",
            order = 2,
        )
        var displaySeq = 128

        fun create(weightsDirectory: String): LanguageModel =
            LanguageModel(weightsDirectory, maxSeqLen, displaySeq)

        override val name = "Language Model"
    }
}
