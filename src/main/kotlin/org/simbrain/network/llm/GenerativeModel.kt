package org.simbrain.network.llm

import org.simbrain.network.compositor.*
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.ProvidesDisplayTokenizer
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D

/**
 * The generation protocol shared by the language model families: the coupling vocabulary
 * ([generatedToken] and [hiddenState] out, [injectText] and the [contextWindow] document sync
 * in) and the feed-queue discipline. Subclasses own the model math — their update/step
 * functions and the protected hooks at the bottom.
 *
 * There is no run mode and no prompt: [canAdvance] derives whether the next workspace
 * iteration moves generation forward — something to feed or continue from, and nothing
 * halting it (a sealed stream, a full window, a spent budget; each family says which of
 * those it has). Pausing is the workspace's job, and the coupled document is the single
 * source of the context: edits through [contextWindow] rebuild it, [clearWindow] empties it,
 * and on reopen the saved document replays itself into the model.
 *
 * Feed queue: [pending] holds token ids waiting to enter the model one per iteration —
 * whatever [injectText] and the subclasses' seeding paths append. While non-empty the
 * model is prefilling; once drained it feeds back its own sampled token. At any decode pause
 * one sampled-but-unfed token may already be in [text], so every injection path must queue it
 * first, keeping the context in [text]'s order.
 *
 * The annotated members are deliberately concrete here and must not be overridden: coupling
 * discovery reads annotations off the most-derived declaration (an un-annotated override
 * silently drops the attribute), and the producer cache resolves [hiddenStateDescription] once
 * for the shared base method, so it too must stay on the base.
 *
 * Interior tiles: both families draw their internals as a compositor scene. The hand edits a
 * weight tile offers ([editWeights], [replaceWeights]), user tile labels, per-tile increments, and
 * the [TileProbe] coupling endpoints live here; each family supplies the scene, its lock, and what
 * an edit means for its state (retraining state for the tiny model, stale caches for the LFM).
 */
abstract class GenerativeModel : LocatableModel(), EditableObject, AttributeContainer,
    ProvidesDisplayTokenizer {

    /** How the next token is chosen from the distribution. */
    abstract var samplingStrategy: SamplingStrategy

    /** Seed plus generated continuation from the current window. */
    var text: String = ""
        protected set

    /**
     * Whether the next workspace iteration advances generation: there is something to feed
     * (or a context to continue) and no family-specific halt applies.
     */
    val canAdvance: Boolean
        get() = (pending.isNotEmpty() || hasContinuation()) && !isHalted()

    /** Token ids waiting to enter the model, one per iteration: the seed, then injections. */
    @Transient
    protected var pending = ArrayDeque<Int>()

    /** Whether the feed queue is non-empty: the model is reading fed text, not generating. */
    val isPromptProcessing: Boolean
        get() = pending.isNotEmpty()

    /** Backing span for [currentTokenSpan]; subclasses update it as they process tokens. */
    @Transient
    protected var currentSpan: IntArray = IntArray(0)

    /** The freshest sampled token, which may not have entered the model's context yet. */
    @Transient
    protected var sampledToken = -1

    @Transient
    protected var lastGenerated = ""

    @Transient
    protected var syncGate = DocumentSyncGate()

    /** Test seam: scripts the sampled token stream when set, bypassing [samplingStrategy]. */
    @Transient
    internal var sampleOverride: (() -> Int)? = null

    override var location: Point2D = Point2D.Double()
        set(value) {
            field = value
            events.locationChanged.fire()
        }

    var weightIncrement by GuiEditable(
        initValue = 0.01,
        label = "Weight increment",
        description = "How far the up and down keys move every weight in a selected weight tile; " +
            "a tile's own increment, set in its dialog, takes precedence",
        min = 0.0,
        increment = 0.001,
        order = 50,
    )

    /** User names for interior tiles by tile id, shown in place of the compositor's titles. */
    var tileLabels: HashMap<String, String> = HashMap()

    /** Per-tile up/down steps by tile id, overriding [weightIncrement]. */
    var tileIncrements: HashMap<String, Double> = HashMap()

    /** Coupling endpoints made from tile menus, each pinned to a layer and head. */
    var tileProbes: ArrayList<TileProbe> = ArrayList()

    override val childrenContainers: List<AttributeContainer>
        get() = tileProbes.onEach { it.host = this }

    /** The interior compositor scene, or null while there is none (LFM weights not loaded). */
    abstract val interiorScene: CompositorScene?

    /** Sequence row of the last processed token, where tile readouts take the current value. */
    protected abstract val readoutRow: Int

    /** Runs [block] holding the lock the model's compute path holds. */
    protected abstract fun <T> withModelLock(block: () -> T): T

    /** Overwrites [tensor] with fresh random weights; each family picks its distribution. */
    protected abstract fun randomizeWeights(tensor: FloatTensor)

    /** Called under the model lock for each weight tensor a hand edit changed. */
    protected open fun onWeightsEdited(tensor: FloatTensor) {}

    /** Called under the model lock once a batch of weight edits is done, to bring activations up to date. */
    protected abstract fun afterWeightEdits()

    /** A token's text, for labels; null while the model cannot decode (weights not loaded). */
    protected abstract fun tokenText(id: Int): String?

    fun readProbe(probe: TileProbe): DoubleArray? = interiorScene?.tiles
        ?.firstOrNull { it.id == probe.tileId }
        ?.readCurrent(probe.pinnedLayer, probe.pinnedHead, readoutRow)

    /** Index of the logit-lens source behind [tile] at [layer], or -1 when the lens doesn't read it. */
    private fun lensSourceIndex(tile: TensorTile, layer: Int): Int {
        val lens = interiorScene?.lens ?: return -1
        val tensor = tile.sourceTensor(layer) ?: return -1
        return lens.sources.indexOfFirst { it.tensor === tensor }
    }

    /** Whether the logit lens reads [tile] at the layer it shows now, so plots of it can carry lens labels. */
    fun hasLensReading(tile: TensorTile): Boolean = lensSourceIndex(tile, tile.pinnableLayer) >= 0

    /**
     * The token the logit lens predicts from the probe's checkpoint for the last processed token,
     * with whitespace made visible; null with the lens off or off the lens's checkpoints.
     */
    fun readLensToken(probe: TileProbe): String? {
        val lens = interiorScene?.lens ?: return null
        val tile = interiorScene?.tiles?.firstOrNull { it.id == probe.tileId } ?: return null
        val index = lensSourceIndex(tile, probe.pinnedLayer).takeIf { it >= 0 } ?: return null
        val reading = lens.currentReading(index) ?: return null
        return tokenText(reading.tokenId)?.let(::visibleToken)
    }

    /** The name a probe shows: the tile's current label, so a rename carries into plot names. */
    fun tileTitle(probe: TileProbe): String? = interiorScene?.tiles?.firstOrNull { it.id == probe.tileId }?.displayTitle

    /**
     * The probe reading [tile] at the layer and head it shows now. An existing probe with the
     * same pins is reused, so two plots of the same view share one endpoint.
     */
    fun probeFor(tile: TensorTile): TileProbe {
        val layer = tile.pinnableLayer
        val head = tile.pinnableHead
        val probe = tileProbes.firstOrNull { it.tileId == tile.id && it.pinnedLayer == layer && it.pinnedHead == head }
            ?: TileProbe(tile.id, layer, head, tile.displayTitle).also { tileProbes.add(it) }
        probe.host = this
        return probe
    }

    fun incrementFor(tile: TensorTile): Double = tileIncrements[tile.id] ?: weightIncrement

    fun setTileIncrement(tile: TensorTile, increment: Double?) {
        if (increment == null) tileIncrements.remove(tile.id) else tileIncrements[tile.id] = increment
    }

    fun setTileLabel(tile: TensorTile, label: String?) {
        val trimmed = label?.trim()?.takeIf { it.isNotEmpty() && it != tile.title }
        if (trimmed == null) tileLabels.remove(tile.id) else tileLabels[tile.id] = trimmed
        tile.label = trimmed
    }

    /** Puts saved user labels on a freshly built scene's tiles. */
    protected fun applyTileLabels(scene: CompositorScene) {
        scene.tiles.forEach { it.label = tileLabels[it.id] }
    }

    /** Applies [edit] to the matrix each weight tile in [tiles] shows; other tiles are ignored. */
    fun editWeights(tiles: Collection<TensorTile>, edit: WeightEdit) {
        applyToWeights(tiles.filterIsInstance<MatrixTile>().filter { it.kind == TileKind.WEIGHT }) { tile, tensor ->
            when (edit) {
                WeightEdit.CLEAR -> tensor.fill(0f)
                WeightEdit.RANDOMIZE -> randomizeWeights(tensor)
                WeightEdit.INCREMENT -> tensor.shiftBy(incrementFor(tile).toFloat())
                WeightEdit.DECREMENT -> tensor.shiftBy(-incrementFor(tile).toFloat())
            }
        }
    }

    /** Overwrites the matrix a weight tile shows with [values], row-major in the tensor's own layout. */
    fun replaceWeights(tile: MatrixTile, values: FloatArray) {
        applyToWeights(listOf(tile)) { _, tensor -> tensor.copyFrom(values) }
    }

    private fun applyToWeights(tiles: List<MatrixTile>, edit: (MatrixTile, FloatTensor) -> Unit) {
        if (tiles.isEmpty()) return
        withModelLock {
            for (tile in tiles) {
                edit(tile, tile.tensor)
                onWeightsEdited(tile.tensor)
            }
            afterWeightEdits()
        }
        tiles.forEach { it.refreshFromSource() }
        events.updated.fire()
    }

    private fun FloatTensor.shiftBy(delta: Float) {
        for (i in 0 until size) data.put(i, data.get(i) + delta)
        markMutated()
    }

    /**
     * Empties the window and run state; the model waits for new text. A coupled non-empty
     * document restores itself on the next play (the document is the truth) — clear the
     * document too for a full reset.
     */
    @Synchronized
    fun clearWindow() {
        onClear()
        pending = ArrayDeque()
        sampledToken = -1
        lastGenerated = ""
        currentSpan = IntArray(0)
        text = ""
        syncGate.reset()
        events.updated.fire()
    }

    /** Text of the token generated this iteration; empty while prefilling or halted. */
    @get:Producible
    val generatedToken: String
        get() = lastGenerated

    /**
     * Char range `[start, end)` in [contextWindow]'s text of the token this iteration
     * processed — the token being read while the feed queue drains, or the freshly sampled
     * token during generation. Empty while idle. Couple to a text world's highlight-span
     * consumer to sweep a highlight across the document as the model works.
     */
    @get:Producible
    val currentTokenSpan: IntArray
        get() = currentSpan

    /** The model's hidden state at the current position; [hiddenStateLabel] says which one. */
    @get:Producible(customDescriptionMethod = "hiddenStateDescription")
    val hiddenState: DoubleArray
        get() = computeHiddenState()

    fun hiddenStateDescription() = "$id:hiddenState (${hiddenStateLabel()})"

    /**
     * Encodes [newText] and appends it to the feed queue, extending prefill: the model walks
     * the injected tokens one per iteration before resuming its own continuation. Feeding the
     * queue is what moves a waiting or sealed model forward. Meant for advancing sources — a
     * static string producer re-injects its value every coupling update.
     */
    @Synchronized
    @Consumable
    fun injectText(newText: String) {
        if (newText.isEmpty()) return
        val ids = encodeText(newText) ?: return
        if (ids.isEmpty()) return
        if (pending.isEmpty() && sampledToken >= 0) pending.addLast(sampledToken)
        ids.forEach { pending.addLast(it) }
        onInjected(newText, ids)
        syncGate.invalidate()
    }

    /**
     * The context window as text — exactly what the model reads. Producing follows an
     * ownership rule: the window is published while the model is advancing, plus until a
     * halted window has echoed back, and is empty otherwise, so a paired document consumer is
     * never clobbered while the user may be editing it. Consuming an unrecognized value is an
     * edit: [applyWindowEdit] rebuilds the context and the model continues from it.
     */
    @get:Producible
    @set:Consumable
    var contextWindow: String
        @Synchronized
        get() {
            val window = windowText() ?: return ""
            return syncGate.publish(window, canAdvance)
        }
        @Synchronized
        set(value) {
            val current = windowText() ?: return
            if (value.isEmpty()) {
                if (syncGate.hasPublishedNonEmptyWindow && (current.isNotEmpty() || canAdvance)) clearWindow()
                return
            }
            if (!syncGate.isEdit(value, current, canAdvance)) return
            val ids = encodeText(value) ?: return
            if (ids.isEmpty()) return
            applyWindowEdit(ids)
            sampledToken = -1
            lastGenerated = ""
            syncGate.invalidate()
            events.updated.fire()
        }

    override suspend fun delete(): List<NetworkModel> {
        onDelete()
        events.deleted.fire(this)
        return listOf(this)
    }

    /** [textIn] as model token ids, or null while the model cannot encode (weights unloaded). */
    protected abstract fun encodeText(textIn: String): IntArray?

    /** The live context window as text, or null while there is no window to publish. */
    protected abstract fun windowText(): String?

    /** Replaces the model's context with an edited window's [ids] and updates [text]. */
    protected abstract fun applyWindowEdit(ids: IntArray)

    /** Resets model-specific state for an empty window. */
    protected abstract fun onClear()

    /** Whether the model has a context to continue from once the feed queue drains. */
    protected abstract fun hasContinuation(): Boolean

    /** Family-specific halts: a sealed stream, a full window, a spent budget. */
    protected open fun isHalted(): Boolean = false

    /** Commits injected tokens: mirrors [ids] into model-side buffers and appends to [text]. */
    protected abstract fun onInjected(newText: String, ids: IntArray)

    protected abstract fun computeHiddenState(): DoubleArray

    /** Which hidden state [hiddenState] produces, for the attribute description. */
    protected abstract fun hiddenStateLabel(): String

    protected open suspend fun onDelete() {}
}
