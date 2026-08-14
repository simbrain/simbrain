package org.simbrain.network.compositor

import org.simbrain.network.compositor.HistoryView.*
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.TensorPort
import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.ceil

/** What a tile shows, driving its colormap and border styling. */
enum class TileKind { ACTIVATION, RESIDUAL, WEIGHT, ATTENTION, GRADIENT }

/** How many recently watched layers a history tile stashes, so flip-back is instant and lossless. */
const val HISTORY_STASH = 3

/**
 * Float budget above which an [AttentionTile] keeps history for the selected head only,
 * re-deriving other heads through the scene's replay path. All-heads retention is quadratic in
 * the context window (numHeads x window^2 floats, ~268 MB at 16 heads and 2048 tokens), so
 * large windows trade instant head flips for bounded memory.
 */
const val ATTENTION_HISTORY_BUDGET: Long = 32L * 1024 * 1024

/** Live-view strength of history rows: faint enough to read as a recording, not model state. */
const val HISTORY_GHOST = 0.15f

/**
 * How the scene treats recorded token history. [FULL] shows the recording at full strength.
 * [GHOSTED] dims history rows to [HISTORY_GHOST], leaving only the live row and resident state
 * bright — recording continues underneath, so toggling back is lossless. [OFF] keeps no history
 * at all: past rows are dropped as the live row advances, flips carry nothing to restore or
 * stash, and leaving the mode re-derives history by replaying from the depth strip, which keeps
 * recording in every mode as the replay source.
 */
enum class HistoryView { FULL, GHOSTED, OFF }

/**
 * A tile whose data source flips across model layers — the card stack behind a structure-first
 * view where one block anatomy is shown once and the layer dimension collapses into decks.
 * [stackLayers] holds the model layer index behind each stack entry (empty for unstacked tiles);
 * [showLayer] flips to a layer's entry instantly, returning false when the stack has no entry
 * for it (e.g. a conv-limb tile while an attention layer is selected).
 */
interface LayerStacked {
    val stackLayers: List<Int>
    val shownLayer: Int
    fun showLayer(layer: Int): Boolean
}

/**
 * The next model layer this stack has an entry for, strictly after [layer], wrapping to the
 * first — a conv tile pages 3 -> 5 past an attention layer instead of landing on a layer it
 * doesn't exist on.
 */
fun LayerStacked.layerAfter(layer: Int): Int? =
    stackLayers.firstOrNull { it > layer } ?: stackLayers.firstOrNull()

/** The previous stack layer strictly before [layer], wrapping to the last. */
fun LayerStacked.layerBefore(layer: Int): Int? =
    stackLayers.lastOrNull { it < layer } ?: stackLayers.lastOrNull()

/**
 * One heatmap in the compositor's retained scene: a layout rect in scene coordinates and a value
 * buffer holding published data. No full-resolution image exists — pixels are produced on demand
 * by [shadePatch], which shades just the visible cell window at screen resolution, so shading
 * cost tracks viewport pixels, never tensor size. [publish] copies fresh values and bumps
 * [contentVersion]; the renderer's patch rebuilds when the version moves, the visible region
 * changes, or the palette switches.
 *
 * [publish] is called from the compute thread and [shadePatch] from the EDT; both synchronize
 * on the tile.
 */
abstract class TensorTile(
    val id: String,
    val title: String,
    val rows: Int,
    val cols: Int,
    private val signedNorm: Boolean,
    val kind: TileKind = TileKind.ACTIVATION,
) : FlowEndpoint {

    /** Learner-facing dimensions for hover text; specialized tiles add semantic axes such as heads. */
    open val tooltipShape: String get() = "$rows rows × $cols columns"

    var x = 0.0
    var y = 0.0
    var width = cols.toDouble()
    var height = rows.toDouble()

    /** True when this tile belongs to a limb the selected layer doesn't use; rendered faded. */
    var dimmed = false

    /**
     * True when the tile is drawn larger than the scene's shared axis scales would allow — a
     * magnified inset (rope angles, single-token rows, conv taps). Rendered with a dashed border
     * so the scale break is explicit instead of silent.
     */
    var magnified = false

    /**
     * Interior column boundaries (in display cell coordinates) of the substructure packed along
     * the feature axis — head boundaries on q/k/v/context, chunk boundaries on a fused
     * projection. Rendered as boundary marks so the packing is countable.
     */
    var columnTicks: List<Int> = emptyList()

    /** Interior row boundaries, e.g. the output blocks of a fused projection's weight matrix. */
    var rowTicks: List<Int> = emptyList()

    /** Labels for the column blocks the [columnTicks] delimit (size = ticks + 1), or empty. */
    var blockLabels: List<String> = emptyList()

    /** Whether column ticks divide the full displayed tensor height instead of only marking its edges. */
    var fullHeightColumnTicks = false

    /**
     * How many independent parallel streams the tile's value is split into — the strand count
     * edges leaving this tile render with. 1 for a flat vector; the per-head count from the
     * first head-aware op (norm+rope) until the heads merge back at the output projection.
     */
    var strands = 1

    /** Whether a whole-tensor tile should show the active sequence row passed to [publish]. */
    var tracksLiveRow = false

    /**
     * True when this tile's rows are a scene-side recording of past tokens rather than model
     * state — the port only ever holds the current token's value. [historyView] governs how
     * (and whether) these rows are kept and shown.
     */
    open val accumulatesHistory = false

    /**
     * True for recording tiles that keep full history in every view mode — the depth strip,
     * whose checkpoints are the source the scene replays block history from. Rendered ghosted
     * in [HistoryView.OFF] instead of dropping rows.
     */
    var alwaysRecords = false

    /**
     * How this tile's recorded history renders — see [HistoryView]. Tiles mirroring real state
     * (weights, caches, full-pass tensors) are unaffected.
     */
    var historyView = HistoryView.FULL
        set(value) {
            if (field == value) return
            field = value
            if (accumulatesHistory) {
                if (value == HistoryView.OFF && !alwaysRecords) retainOnlyLiveRow()
                touch()
            }
        }

    /** True when this tile drops past rows instead of recording them. */
    protected val dropsHistory get() =
        accumulatesHistory && !alwaysRecords && historyView == HistoryView.OFF

    /**
     * The row holding the current token's just-published value, or -1 when rows aren't a token
     * axis (full-pass publishes). Rendered as a cursor: on history tiles it marks the one row
     * actually flowing through the graph this step; on the KV caches it marks the write frontier
     * (rows past it are stale).
     */
    var liveRow = -1
        protected set(value) {
            if (field != value && field >= 0 && accumulatesHistory && historyView != HistoryView.FULL) {
                // The outgoing row drops to ghost strength (or clears), so it must reshade.
                // Always-recording tiles ghost in OFF view too, but never clear: real memory.
                if (historyView == HistoryView.OFF && !alwaysRecords) clearRow(field)
                touchRow(field)
            }
            field = value
        }

    /** Zeroes one outgoing row when history is off; subclasses clear parallel buffers too. */
    protected open fun clearRow(row: Int) {
        values.fill(0f, row * cols, (row + 1) * cols)
    }

    /** Drops recorded history to just the live row — entering [HistoryView.OFF]. */
    @Synchronized
    protected open fun retainOnlyLiveRow() {
        for (r in 0 until rows) {
            if (r != liveRow) values.fill(0f, r * cols, (r + 1) * cols)
        }
    }

    init {
        require(rows.toLong() * cols <= Int.MAX_VALUE) {
            "Tile $id shape $rows x $cols overflows its backing buffer"
        }
    }

    /** Published data, row-major [rows] x [cols]. Read for tooltips and probes; written by [publish]. */
    val values = FloatArray(rows * cols)

    /** Bumped on any change to [values] or the color scale; the renderer repaints when it moves. */
    @Volatile
    var contentVersion = 0L
        private set

    private var dirtyAll = true
    private var dirtyFrom = Int.MAX_VALUE
    private var dirtyTo = -1

    protected fun touch() {
        dirtyAll = true
        contentVersion++
    }

    /** Marks a single-row change (a token's publish), letting the renderer reshade just its band. */
    protected fun touchRow(row: Int) {
        if (!dirtyAll) {
            dirtyFrom = minOf(dirtyFrom, row)
            dirtyTo = maxOf(dirtyTo, row)
        }
        contentVersion++
    }

    /**
     * The cell rows dirtied since the last consume, cleared on return — null means everything
     * (bulk writes, scale growth, flips). Single consumer: the tile's patch renderer.
     */
    @Synchronized
    fun consumeDirtyRows(): IntRange? {
        val result = if (dirtyAll || dirtyTo < dirtyFrom) null else dirtyFrom..dirtyTo
        dirtyAll = false
        dirtyFrom = Int.MAX_VALUE
        dirtyTo = -1
        return result
    }

    protected var absMax = 0f
        private set

    /** Copies this token's fresh values out of the source tensor and bumps [contentVersion]. */
    abstract fun publish(tokenIndex: Int)

    /** Clears published history for a fresh generation run. */
    @Synchronized
    open fun reset() {
        values.fill(0f)
        absMax = 0f
        liveRow = -1
        touch()
    }

    /**
     * Clears recorded rows at and beyond [row] after a prefix-reuse rewind: positions before
     * [row] stay valid and the rewound positions will be republished as the model re-feeds (or
     * never return, if the new stream ends earlier). Only scene-side recordings need clearing —
     * tiles mirroring live model state re-copy from their source on the next publish.
     */
    @Synchronized
    open fun truncateFrom(row: Int) {
        if (!accumulatesHistory || row >= rows) return
        val from = row.coerceAtLeast(0)
        values.fill(0f, from * cols, rows * cols)
        if (liveRow >= from) liveRow = if (from > 0) from - 1 else -1
        touch()
    }

    /** Forgets the signed-normalization scale — for view switches to differently-scaled data. */
    protected fun resetScale() {
        absMax = 0f
        touch()
    }

    /** Grows the signed-normalization scale; it never shrinks mid-run, so shading stays monotone. */
    protected fun growAbsMax(candidate: Float) {
        if (candidate > absMax) {
            absMax = candidate
            touch()
        }
    }

    /**
     * Shades the patch pixels covering the fractional cell window rows [rowFrom, rowTo) x
     * cols [colFrom, colTo) into [dest] — [destW] x [destH] pixels, row-major with [stride].
     * When several cells collapse into one pixel, the pool depends on what the tile shows:
     * activations and attention keep the largest-magnitude cell (sign intact), so sparse spikes
     * survive downsampling; weights average the magnitudes (signed by the block average), so a
     * dense matrix reads by its norm structure instead of clipping every pixel to an outlier.
     * Both rules converge to the true signed cell value at one-cell-per-pixel zoom.
     */
    @Synchronized
    fun shadePatch(
        dest: IntArray, stride: Int, destW: Int, destH: Int,
        rowFrom: Double, rowTo: Double, colFrom: Double, colTo: Double,
        neg: Color, mid: Color, pos: Color,
        destOffset: Int = 0,
    ) {
        val scale = if (signedNorm) (if (absMax > 0f) 1f / absMax else 0f) else 1f
        val meanPool = kind == TileKind.WEIGHT
        // With history off, non-strip tiles hold only the live row, so nothing needs dimming;
        // the always-recording depth strip ghosts instead so resident state alone reads bright.
        val ghosting = accumulatesHistory && (historyView == HistoryView.GHOSTED ||
            (historyView == HistoryView.OFF && alwaysRecords))
        val rowSpan = rowTo - rowFrom
        val colSpan = colTo - colFrom
        for (y in 0 until destH) {
            val r0 = (rowFrom + rowSpan * y / destH).toInt().coerceIn(0, rows - 1)
            val r1 = ceil(rowFrom + rowSpan * (y + 1) / destH).toInt().coerceIn(r0 + 1, rows)
            val ghostBand = ghosting && liveRow !in r0 until r1
            val destRow = destOffset + y * stride
            for (x in 0 until destW) {
                val c0 = (colFrom + colSpan * x / destW).toInt().coerceIn(0, cols - 1)
                val c1 = ceil(colFrom + colSpan * (x + 1) / destW).toInt().coerceIn(c0 + 1, cols)
                var pooled = 0f
                if (meanPool) {
                    var sum = 0f
                    var sumAbs = 0f
                    for (r in r0 until r1) {
                        val base = r * cols
                        for (c in c0 until c1) {
                            val v = values[base + c]
                            sum += v
                            sumAbs += abs(v)
                        }
                    }
                    val mag = sumAbs / ((r1 - r0) * (c1 - c0))
                    pooled = if (sum < 0) -mag else mag
                } else {
                    var bestAbs = -1f
                    for (r in r0 until r1) {
                        val base = r * cols
                        for (c in c0 until c1) {
                            val v = values[base + c]
                            val a = abs(v)
                            if (a > bestAbs) {
                                bestAbs = a
                                pooled = v
                            }
                        }
                    }
                }
                val shown = pooled * scale * (if (ghostBand) HISTORY_GHOST else 1f)
                dest[destRow + x] = shown.toSimbrainColor(neg, mid, pos)
            }
        }
    }

    fun contains(sceneX: Double, sceneY: Double) =
        sceneX >= x && sceneX < x + width && sceneY >= y && sceneY < y + height

    val bounds: Rectangle2D get() = Rectangle2D.Double(x, y, width, height)

    fun intersects(rx: Double, ry: Double, rw: Double, rh: Double) =
        rx < x + width && rx + rw > x && ry < y + height && ry + rh > y

    /** Maps a scene point inside this tile to a (row, col) data cell, or null when outside. */
    fun cellAt(sceneX: Double, sceneY: Double): Pair<Int, Int>? {
        if (!contains(sceneX, sceneY)) return null
        val row = ((sceneY - y) / height * rows).toInt().coerceIn(0, rows - 1)
        val col = ((sceneX - x) / width * cols).toInt().coerceIn(0, cols - 1)
        return row to col
    }

    fun valueAt(row: Int, col: Int) = values[row * cols + col]
}

/**
 * A tile that accumulates a vector-valued port token by token: row t is the port's value after
 * token t. This is the residual-stream view — the port holds only the current token's vector,
 * so history lives here in the scene. Signed data, normalized by a running high quantile of the
 * magnitudes rather than the max: transformer residual streams grow a few huge outlier channels,
 * and max scaling would wash every other channel out. Outliers saturate instead.
 *
 * With several [ports] the tile is a layer stack, and history is recorded for the watched layer
 * only: [showLayer] stashes the outgoing layer's rows (an LRU of [HISTORY_STASH] recently watched
 * layers, so flip-back is instant and lossless) and either restores the incoming layer's or
 * starts blank — the scene then re-derives rows ([backfillRow]) by replaying the block through
 * the real ops from state it still holds. The normalization scale is shared across everything
 * the tile has shown.
 */
class VectorHistoryTile(
    val ports: List<TensorPort>,
    rows: Int,
    title: String = ports.first().name,
    kind: TileKind = TileKind.RESIDUAL,
    id: String = ports.first().name,
    override val stackLayers: List<Int> = emptyList(),
) : TensorTile(id, title, rows, ports.first().tensor.size, signedNorm = true, kind = kind), LayerStacked {

    constructor(port: TensorPort, rows: Int, title: String = port.name, kind: TileKind = TileKind.RESIDUAL) :
        this(listOf(port), rows, title, kind)

    override val accumulatesHistory = true

    init {
        require(stackLayers.isEmpty() || stackLayers.size == ports.size) {
            "Stack layers (${stackLayers.size}) must match ports (${ports.size})"
        }
        require(ports.all { it.tensor.size == cols }) { "All stacked ports must share one width" }
    }

    private val stash = LinkedHashMap<Int, FloatArray>()
    private val stashedAt = HashMap<Int, Int>()
    private val lastVersions = LongArray(ports.size) { -1L }
    private val magnitudes = FloatArray(cols)
    private var selected = 0

    override val shownLayer get() = stackLayers.getOrElse(selected) { -1 }

    @Synchronized
    override fun showLayer(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index != selected) {
            if (dropsHistory) {
                // Nothing to stash or restore: reseed the live row from the new layer's port.
                selected = index
                values.fill(0f)
                reseedLiveRow()
            } else {
                // Restore before stashing, so the incoming layer is never the eviction victim.
                val restored = stash.remove(index)
                stashedAt.remove(index)
                if (stash.size >= HISTORY_STASH) stash.remove(stash.keys.first())
                stash[selected] = values.copyOf()
                stashedAt[selected] = liveRow
                selected = index
                if (restored != null) System.arraycopy(restored, 0, values, 0, values.size)
                else values.fill(0f)
            }
            touch()
        }
        return true
    }

    /** Tokens published past a stash's capture point leave it permanently short; drop it. */
    private fun dropStaleStashes(tokenIndex: Int) {
        if (stash.isEmpty()) return
        val keys = stash.keys.iterator()
        while (keys.hasNext()) {
            val key = keys.next()
            if ((stashedAt[key] ?: -1) < tokenIndex) keys.remove()
        }
        stashedAt.keys.retainAll(stash.keys)
    }

    private fun reseedLiveRow() {
        if (liveRow !in 0 until rows) return
        val tensor = ports[selected].tensor
        lastVersions[selected] = tensor.version
        val base = liveRow * cols
        for (i in 0 until cols) {
            val v = tensor.data.get(i)
            values[base + i] = v
            magnitudes[i] = abs(v)
        }
        growScaleFromMagnitudes()
    }

    @Synchronized
    override fun retainOnlyLiveRow() {
        super.retainOnlyLiveRow()
        stash.clear()
        stashedAt.clear()
    }

    /** Stashed layers hold rewound rows too, so a rewind drops them; flips re-replay instead. */
    @Synchronized
    override fun truncateFrom(row: Int) {
        if (row >= rows) return
        super.truncateFrom(row)
        stash.clear()
        stashedAt.clear()
    }

    /** True when [layer]'s history is already in memory — shown or stashed. */
    @Synchronized
    fun hasHistoryFor(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        return index >= 0 && (index == selected || index in stash)
    }

    override fun reset() {
        super.reset()
        stash.clear()
        stashedAt.clear()
        lastVersions.fill(-1L)
    }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        if (tokenIndex !in 0 until rows) return
        liveRow = tokenIndex
        dropStaleStashes(tokenIndex)
        val tensor = ports[selected].tensor
        if (tensor.version == lastVersions[selected]) return
        lastVersions[selected] = tensor.version
        val base = tokenIndex * cols
        for (i in 0 until cols) {
            val v = tensor.data.get(i)
            values[base + i] = v
            magnitudes[i] = abs(v)
        }
        growScaleFromMagnitudes()
        touchRow(tokenIndex)
    }

    /** Writes a re-derived or copied history row for the shown layer — the flip-backfill path. */
    @Synchronized
    fun backfillRow(tokenIndex: Int, src: FloatArray, srcOffset: Int = 0) {
        if (tokenIndex !in 0 until rows) return
        val base = tokenIndex * cols
        for (i in 0 until cols) {
            val v = src[srcOffset + i]
            values[base + i] = v
            magnitudes[i] = abs(v)
        }
        growScaleFromMagnitudes()
        touchRow(tokenIndex)
    }

    /** Backfills straight from a replay scratch tensor, laid out like the live port. */
    @Synchronized
    fun backfillRow(tokenIndex: Int, src: FloatTensor) {
        if (tokenIndex !in 0 until rows) return
        val base = tokenIndex * cols
        for (i in 0 until cols) {
            val v = src.data.get(i)
            values[base + i] = v
            magnitudes[i] = abs(v)
        }
        growScaleFromMagnitudes()
        touchRow(tokenIndex)
    }

    private fun growScaleFromMagnitudes() {
        magnitudes.sort()
        val quantile = magnitudes[(cols.toLong() * 995 / 1000).toInt().coerceAtMost(cols - 1)]
        growAbsMax(if (quantile > 0f) quantile else magnitudes[cols - 1])
    }
}

/**
 * A tile mirroring a whole 2-D tensor that changes in bulk — the full-sequence teaching pass,
 * where every forward (or training step, for weights) rewrites the entire matrix. [publish]
 * ignores the token index: it is version-gated on the source tensor and copies everything.
 * [displayTransposed] renders the transpose (weight tiles honoring Simbrain's source-target
 * display convention); [quantileNorm] normalizes by a high magnitude quantile instead of the
 * max (residual checkpoints, whose outlier channels would wash everything else out).
 */
class MatrixTile(
    id: String,
    title: String,
    val tensors: List<FloatTensor>,
    kind: TileKind = TileKind.ACTIVATION,
    signedNorm: Boolean = true,
    private val quantileNorm: Boolean = false,
    private val versionGated: Boolean = true,
    private val displayTransposed: Boolean = false,
    override val stackLayers: List<Int> = emptyList(),
) : TensorTile(
    id, title,
    if (displayTransposed) tensors.first().cols else tensors.first().rows,
    if (displayTransposed) tensors.first().rows else tensors.first().cols,
    signedNorm, kind,
), LayerStacked {

    constructor(
        id: String,
        title: String,
        tensor: FloatTensor,
        kind: TileKind = TileKind.ACTIVATION,
        signedNorm: Boolean = true,
        quantileNorm: Boolean = false,
        versionGated: Boolean = true,
        displayTransposed: Boolean = false,
    ) : this(id, title, listOf(tensor), kind, signedNorm, quantileNorm, versionGated, displayTransposed)

    constructor(
        port: TensorPort,
        kind: TileKind = TileKind.ACTIVATION,
        title: String = port.name,
        signedNorm: Boolean = true,
        quantileNorm: Boolean = false,
        displayTransposed: Boolean = false,
    ) : this(port.name, title, listOf(port.tensor), kind, signedNorm, quantileNorm, true, displayTransposed)

    init {
        require(stackLayers.isEmpty() || stackLayers.size == tensors.size) {
            "Stack layers (${stackLayers.size}) must match tensors (${tensors.size})"
        }
        require(tensors.all { it.rows == tensors.first().rows && it.cols == tensors.first().cols }) {
            "All stacked tensors must share one shape"
        }
    }

    private var selected = 0

    /** The currently displayed source. Unstacked tiles have exactly one. */
    val tensor: FloatTensor get() = tensors[selected]

    override val shownLayer get() = stackLayers.getOrElse(selected) { -1 }

    /** Sources persist (weights, rolling caches), so flipping just re-copies the new one. */
    @Synchronized
    override fun showLayer(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index != selected) {
            selected = index
            lastVersion = -1L
            publish(-1)
        }
        return true
    }

    private var lastVersion = -1L

    /**
     * The tile's gradient buffer, when a scene supports the training-mode gradient view. Not
     * version-gated: VJPs accumulate into gradient buffers through hot loops that don't bump
     * versions, so gradient publishes always re-copy.
     */
    var gradientSource: FloatTensor? = null

    var showingGradient = false
        set(value) {
            if (field == value) return
            field = value
            lastVersion = -1L
            resetScale()
        }

    override fun reset() {
        super.reset()
        lastVersion = -1L
    }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        val source = if (showingGradient) gradientSource ?: return else tensor
        val gate = versionGated && !showingGradient
        if (tracksLiveRow) liveRow = tokenIndex.takeIf { it in 0 until rows } ?: -1
        if (gate && source.version == lastVersion) return
        lastVersion = source.version
        if (displayTransposed) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    values[r * cols + c] = source.data.get(c * source.cols + r)
                }
            }
        } else {
            for (i in values.indices) values[i] = source.data.get(i)
        }
        growAbsMax(normalizationScale())
        touch()
    }

    private fun normalizationScale(): Float {
        if (!quantileNorm) {
            var max = 0f
            for (v in values) max = maxOf(max, abs(v))
            return max
        }
        val magnitudes = FloatArray(values.size) { abs(values[it]) }
        magnitudes.sort()
        val quantile = magnitudes[(magnitudes.size.toLong() * 995 / 1000).toInt().coerceAtMost(magnitudes.size - 1)]
        return if (quantile > 0f) quantile else magnitudes[magnitudes.size - 1]
    }
}

/**
 * A stacked rank-3 tile: the source tensor holds [slices] stacked blocks — row blocks by default
 * (slice s occupying rows [s*rows, (s+1)*rows)): per-head attention maps, per-head projections —
 * or column blocks with [columnSlices] (slice s occupying columns [s*cols, (s+1)*cols)): KV
 * caches laid out positions x (heads · headDim). The tile renders one slice at a time; every
 * slice is retained in a cube on publish, so [selectedSlice] flips instantly by reshading from
 * memory with zero recompute. Full-pass semantics like [MatrixTile]: publish is version-gated
 * and ignores the token index.
 */
class DeckTile(
    id: String,
    title: String,
    val tensors: List<FloatTensor>,
    val slices: Int,
    kind: TileKind = TileKind.ATTENTION,
    signedNorm: Boolean = false,
    private val columnSlices: Boolean = false,
    override val stackLayers: List<Int> = emptyList(),
) : TensorTile(
    id, title,
    if (columnSlices) tensors.first().rows else tensors.first().rows / slices,
    if (columnSlices) tensors.first().cols / slices else tensors.first().cols,
    signedNorm, kind,
), LayerStacked {

    override val tooltipShape: String get() = "$slices heads × $rows rows × $cols columns"

    init {
        strands = slices
    }

    constructor(
        id: String,
        title: String,
        tensor: FloatTensor,
        slices: Int,
        kind: TileKind = TileKind.ATTENTION,
        signedNorm: Boolean = false,
        columnSlices: Boolean = false,
    ) : this(id, title, listOf(tensor), slices, kind, signedNorm, columnSlices)

    constructor(
        port: TensorPort,
        slices: Int,
        kind: TileKind = TileKind.ATTENTION,
        title: String = port.name,
        signedNorm: Boolean = false,
        columnSlices: Boolean = false,
    ) : this(port.name, title, listOf(port.tensor), slices, kind, signedNorm, columnSlices)

    init {
        require(stackLayers.isEmpty() || stackLayers.size == tensors.size) {
            "Stack layers (${stackLayers.size}) must match tensors (${tensors.size})"
        }
        require(tensors.all { it.rows == tensors.first().rows && it.cols == tensors.first().cols }) {
            "All stacked tensors must share one shape"
        }
        if (columnSlices) {
            require(tensors.first().cols % slices == 0) { "${tensors.first().cols} cols not divisible into $slices slices" }
        } else {
            require(tensors.first().rows % slices == 0) { "${tensors.first().rows} rows not divisible into $slices slices" }
        }
    }

    private var selected = 0

    /** The currently displayed source. Unstacked tiles have exactly one. */
    val tensor: FloatTensor get() = tensors[selected]

    override val shownLayer get() = stackLayers.getOrElse(selected) { -1 }

    /** Cache tensors persist in the model, so flipping re-copies the new layer's live state. */
    @Synchronized
    override fun showLayer(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index != selected) {
            selected = index
            lastVersion = -1L
            publish(-1)
        }
        return true
    }

    private val cube = FloatArray(tensors.first().rows * tensors.first().cols)
    private var lastVersion = -1L

    /** Additional hover text for the selected slice, such as a grouped-query head mapping. */
    var sliceTooltip: ((Int) -> String)? = null

    var selectedSlice = 0
        set(value) {
            require(value in 0 until slices) { "Slice $value out of range 0..${slices - 1}" }
            if (field != value) {
                field = value
                rebuildFromCube()
            }
        }

    override fun reset() {
        super.reset()
        cube.fill(0f)
        lastVersion = -1L
    }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        if (tensor.version == lastVersion) return
        // A cache deck's steady-state delta is one written row per token; a stale tile
        // (first publish, layer flip) re-copies with no such guarantee.
        val incremental = columnSlices && lastVersion != -1L && tokenIndex in 0 until rows
        lastVersion = tensor.version
        if (tokenIndex in 0 until rows) liveRow = tokenIndex
        for (i in cube.indices) cube[i] = tensor.data.get(i)
        var max = 0f
        for (v in cube) max = maxOf(max, abs(v))
        growAbsMax(max)
        copySlice()
        if (incremental) touchRow(tokenIndex) else touch()
    }

    @Synchronized
    private fun rebuildFromCube() {
        copySlice()
        touch()
    }

    private fun copySlice() {
        if (columnSlices) {
            for (r in 0 until rows) {
                System.arraycopy(cube, r * tensor.cols + selectedSlice * cols, values, r * cols, cols)
            }
        } else {
            System.arraycopy(cube, selectedSlice * rows * cols, values, 0, rows * cols)
        }
    }
}

/**
 * The causal attention-map tile: row t holds the selected head's softmaxed attention over
 * positions 0..t at token t, building the familiar lower triangle as generation runs. All heads
 * of the watched layer are retained, so [selectedHead] switches instantly with no recompute.
 * Across layers it records the watched layer only: [showLayer] stashes the outgoing layer's
 * heads (LRU of [HISTORY_STASH]) and restores or blanks the incoming one, after which the scene
 * re-derives missing rows by replaying the block against the KV caches through [backfillRow].
 * Weights are probabilities, shaded on a fixed 0..1 scale.
 */
class AttentionTile(
    val ports: List<TensorPort>,
    val numHeads: Int,
    val keyValueHeads: Int = numHeads,
    seqLen: Int,
    title: String = ports.first().name,
    id: String = ports.first().name,
    override val stackLayers: List<Int> = emptyList(),
    historyBudget: Long = ATTENTION_HISTORY_BUDGET,
) : TensorTile(id, title, seqLen, seqLen, signedNorm = false, kind = TileKind.ATTENTION), LayerStacked {

    override val tooltipShape: String get() =
        "$numHeads query heads → $keyValueHeads key/value heads; $rows query tokens × $cols key tokens"

    override val accumulatesHistory = true

    init {
        strands = numHeads
    }

    constructor(port: TensorPort, numHeads: Int, seqLen: Int, title: String = port.name) :
        this(listOf(port), numHeads, seqLen = seqLen, title = title)

    init {
        require(stackLayers.isEmpty() || stackLayers.size == ports.size) {
            "Stack layers (${stackLayers.size}) must match ports (${ports.size})"
        }
    }

    /**
     * All-heads retention only under the budget: above it, [values] is the sole record — for
     * [selectedHead] — and flips to other heads re-derive through the scene's replay path.
     */
    private val fullHeadHistory = numHeads.toLong() * rows * cols <= historyBudget

    private val history = FloatArray(if (fullHeadHistory) numHeads * rows * cols else 0)
    private val stash = LinkedHashMap<Int, FloatArray>()
    private val stashedAt = HashMap<Int, Int>()
    private val stashHeads = HashMap<Int, Int>()
    private var lastVersion = -1L
    private var selected = 0
    private var recordedHead = 0

    /** Fires when a single-head flip drops the recorded rows; the host re-derives via replay. */
    var onHeadDataDropped: ((Int) -> Unit)? = null

    private fun payload() = if (fullHeadHistory) history else values

    override val shownLayer get() = stackLayers.getOrElse(selected) { -1 }

    @Synchronized
    override fun showLayer(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index != selected) {
            if (dropsHistory) {
                // Nothing to stash or restore: reseed the live row from the new layer's port.
                selected = index
                history.fill(0f)
                if (!fullHeadHistory) values.fill(0f)
                lastVersion = -1L
                if (liveRow in 0 until rows) {
                    lastVersion = ports[selected].tensor.version
                    record(liveRow, ports[selected].tensor)
                }
                if (fullHeadHistory) rebuildFromHistory() else touch()
            } else {
                // Restore before stashing, so the incoming layer is never the eviction victim.
                val restored = stash.remove(index)
                val restoredHead = stashHeads.remove(index)
                stashedAt.remove(index)
                if (stash.size >= HISTORY_STASH) {
                    val evicted = stash.keys.first()
                    stash.remove(evicted)
                    stashedAt.remove(evicted)
                    stashHeads.remove(evicted)
                }
                stash[selected] = payload().copyOf()
                stashedAt[selected] = liveRow
                stashHeads[selected] = recordedHead
                selected = index
                if (restored != null) {
                    System.arraycopy(restored, 0, payload(), 0, restored.size)
                    if (!fullHeadHistory && restoredHead != null) recordedHead = restoredHead
                } else {
                    payload().fill(0f)
                }
                lastVersion = -1L
                if (fullHeadHistory) rebuildFromHistory() else touch()
            }
        }
        return true
    }

    /** Tokens published past a stash's capture point leave it permanently short; drop it. */
    private fun dropStaleStashes(tokenIndex: Int) {
        if (stash.isEmpty()) return
        val keys = stash.keys.iterator()
        while (keys.hasNext()) {
            val key = keys.next()
            if ((stashedAt[key] ?: -1) < tokenIndex) keys.remove()
        }
        stashedAt.keys.retainAll(stash.keys)
        stashHeads.keys.retainAll(stash.keys)
    }

    override fun clearRow(row: Int) {
        super.clearRow(row)
        if (!fullHeadHistory) return
        for (head in 0 until numHeads) {
            history.fill(0f, (head * rows + row) * cols, (head * rows + row + 1) * cols)
        }
    }

    @Synchronized
    override fun retainOnlyLiveRow() {
        super.retainOnlyLiveRow()
        stash.clear()
        stashedAt.clear()
        stashHeads.clear()
        if (!fullHeadHistory) return
        for (head in 0 until numHeads) {
            for (r in 0 until rows) {
                if (r != liveRow) history.fill(0f, (head * rows + r) * cols, (head * rows + r + 1) * cols)
            }
        }
    }

    /** Stashed layers hold rewound rows too, so a rewind drops them; flips re-replay instead. */
    @Synchronized
    override fun truncateFrom(row: Int) {
        if (row >= rows) return
        super.truncateFrom(row)
        val from = row.coerceAtLeast(0)
        if (fullHeadHistory) {
            for (head in 0 until numHeads) {
                history.fill(0f, (head * rows + from) * cols, (head + 1) * rows * cols)
            }
        }
        stash.clear()
        stashedAt.clear()
        stashHeads.clear()
    }

    /** True when [layer]'s history for the current head is already in memory — shown or stashed. */
    @Synchronized
    fun hasHistoryFor(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index == selected) return true
        if (index !in stash) return false
        return fullHeadHistory || stashHeads[index] == selectedHead
    }

    /** True when [head]'s rows are in memory: always in full retention, else only the recorded head. */
    @Synchronized
    fun hasHeadHistory(head: Int): Boolean = fullHeadHistory || head == recordedHead

    override fun reset() {
        super.reset()
        history.fill(0f)
        stash.clear()
        stashedAt.clear()
        stashHeads.clear()
        lastVersion = -1L
        recordedHead = selectedHead
    }

    var selectedHead = 0
        set(value) {
            require(value in 0 until numHeads) { "Head $value out of range 0..${numHeads - 1}" }
            if (field != value) {
                field = value
                if (fullHeadHistory) rebuildFromHistory() else dropHeadRows()
            }
        }

    /** Single-head flip: the recorded rows are for another head; reseed the live row and ask
     *  the host to re-derive the rest. */
    @Synchronized
    private fun dropHeadRows() {
        values.fill(0f)
        recordedHead = selectedHead
        if (liveRow in 0 until rows) {
            lastVersion = ports[selected].tensor.version
            record(liveRow, ports[selected].tensor)
        }
        touch()
        onHeadDataDropped?.invoke(selectedHead)
    }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        if (tokenIndex !in 0 until rows) return
        liveRow = tokenIndex
        dropStaleStashes(tokenIndex)
        val tensor = ports[selected].tensor
        if (tensor.version == lastVersion) return
        lastVersion = tensor.version
        record(tokenIndex, tensor)
    }

    /** Writes a re-derived history row for the shown layer, laid out like the live weights port. */
    @Synchronized
    fun backfillRow(tokenIndex: Int, source: FloatTensor) {
        if (tokenIndex !in 0 until rows) return
        record(tokenIndex, source)
    }

    private fun record(tokenIndex: Int, tensor: FloatTensor) {
        val seen = minOf(tokenIndex + 1, cols)
        if (fullHeadHistory) {
            for (head in 0 until numHeads) {
                val src = head * tensor.cols
                val dst = (head * rows + tokenIndex) * cols
                for (j in 0 until seen) {
                    history[dst + j] = tensor.data.get(src + j)
                }
            }
            System.arraycopy(
                history, (selectedHead * rows + tokenIndex) * cols,
                values, tokenIndex * cols, cols
            )
        } else {
            val src = selectedHead * tensor.cols
            val dst = tokenIndex * cols
            for (j in 0 until cols) {
                values[dst + j] = if (j < seen) tensor.data.get(src + j) else 0f
            }
            recordedHead = selectedHead
        }
        touchRow(tokenIndex)
    }

    @Synchronized
    private fun rebuildFromHistory() {
        if (!fullHeadHistory) return
        System.arraycopy(history, selectedHead * rows * cols, values, 0, rows * cols)
        touch()
    }
}
