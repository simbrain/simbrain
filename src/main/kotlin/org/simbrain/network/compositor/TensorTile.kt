package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.TensorPort
import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.ceil

/** What a tile shows, driving its colormap and border styling. */
enum class TileKind { ACTIVATION, RESIDUAL, WEIGHT, ATTENTION, GRADIENT }

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

    var x = 0.0
    var y = 0.0
    var width = cols.toDouble()
    var height = rows.toDouble()

    /** True when this tile belongs to a limb the selected layer doesn't use; rendered faded. */
    var dimmed = false

    /**
     * The row holding the current token's just-published value, or -1 when rows aren't a token
     * axis (full-pass publishes). Rendered as a cursor: on history tiles it marks the one row
     * actually flowing through the graph this step; on the KV caches it marks the write frontier
     * (rows past it are stale).
     */
    var liveRow = -1
        protected set

    /** Published data, row-major [rows] x [cols]. Read for tooltips and probes; written by [publish]. */
    val values = FloatArray(rows * cols)

    /** Bumped on any change to [values] or the color scale; the renderer repaints when it moves. */
    @Volatile
    var contentVersion = 0L
        private set

    protected fun touch() {
        contentVersion++
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
     * When several cells collapse into one pixel, the cell with the largest magnitude wins
     * (keeping its sign), so downsampling never hides a spike or an outlier channel.
     */
    @Synchronized
    fun shadePatch(
        dest: IntArray, stride: Int, destW: Int, destH: Int,
        rowFrom: Double, rowTo: Double, colFrom: Double, colTo: Double,
        neg: Color, mid: Color, pos: Color,
    ) {
        val scale = if (signedNorm) (if (absMax > 0f) 1f / absMax else 0f) else 1f
        val rowSpan = rowTo - rowFrom
        val colSpan = colTo - colFrom
        for (y in 0 until destH) {
            val r0 = (rowFrom + rowSpan * y / destH).toInt().coerceIn(0, rows - 1)
            val r1 = ceil(rowFrom + rowSpan * (y + 1) / destH).toInt().coerceIn(r0 + 1, rows)
            val destRow = y * stride
            for (x in 0 until destW) {
                val c0 = (colFrom + colSpan * x / destW).toInt().coerceIn(0, cols - 1)
                val c1 = ceil(colFrom + colSpan * (x + 1) / destW).toInt().coerceIn(c0 + 1, cols)
                var best = 0f
                var bestAbs = -1f
                for (r in r0 until r1) {
                    val base = r * cols
                    for (c in c0 until c1) {
                        val v = values[base + c]
                        val a = abs(v)
                        if (a > bestAbs) {
                            bestAbs = a
                            best = v
                        }
                    }
                }
                dest[destRow + x] = (best * scale).toSimbrainColor(neg, mid, pos)
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
 * With several [ports] the tile is a layer stack: every port's history is retained in a cube on
 * publish (the ports hold only the current token, so history can't be rebuilt later), one layer
 * is displayed, and [showLayer] flips by reshading from memory. The normalization scale is
 * shared across the stack, so flipping through layers compares them on one color scale.
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

    init {
        require(stackLayers.isEmpty() || stackLayers.size == ports.size) {
            "Stack layers (${stackLayers.size}) must match ports (${ports.size})"
        }
        require(ports.all { it.tensor.size == cols }) { "All stacked ports must share one width" }
    }

    private val cube = FloatArray(ports.size * rows * cols)
    private val lastVersions = LongArray(ports.size) { -1L }
    private val magnitudes = FloatArray(cols)
    private var selected = 0

    override val shownLayer get() = stackLayers.getOrElse(selected) { -1 }

    @Synchronized
    override fun showLayer(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index != selected) {
            selected = index
            rebuildFromCube()
        }
        return true
    }

    override fun reset() {
        super.reset()
        cube.fill(0f)
        lastVersions.fill(-1L)
    }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        if (tokenIndex !in 0 until rows) return
        liveRow = tokenIndex
        for ((s, port) in ports.withIndex()) {
            val tensor = port.tensor
            if (tensor.version == lastVersions[s]) continue
            lastVersions[s] = tensor.version
            val base = (s * rows + tokenIndex) * cols
            for (i in 0 until cols) {
                val v = tensor.data.get(i)
                cube[base + i] = v
                magnitudes[i] = abs(v)
            }
            magnitudes.sort()
            val quantile = magnitudes[(cols.toLong() * 995 / 1000).toInt().coerceAtMost(cols - 1)]
            growAbsMax(if (quantile > 0f) quantile else magnitudes[cols - 1])
            if (s == selected) {
                System.arraycopy(cube, base, values, tokenIndex * cols, cols)
                touch()
            }
        }
    }

    @Synchronized
    private fun rebuildFromCube() {
        System.arraycopy(cube, selected * rows * cols, values, 0, rows * cols)
        touch()
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

    /** Custom label per selected slice; null falls back to "title · head N". */
    var sliceLabel: ((Int) -> String)? = null

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
        lastVersion = tensor.version
        if (tokenIndex in 0 until rows) liveRow = tokenIndex
        for (i in cube.indices) cube[i] = tensor.data.get(i)
        var max = 0f
        for (v in cube) max = maxOf(max, abs(v))
        growAbsMax(max)
        copySlice()
        touch()
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
 * positions 0..t at token t, building the familiar lower triangle as generation runs. History
 * for every head is retained, so [selectedHead] switches instantly by reshading from stored
 * values (a tier-2 full-tile rewrite, no recompute). Weights are probabilities, shaded on a
 * fixed 0..1 scale.
 */
class AttentionTile(
    val ports: List<TensorPort>,
    val numHeads: Int,
    seqLen: Int,
    title: String = ports.first().name,
    id: String = ports.first().name,
    override val stackLayers: List<Int> = emptyList(),
) : TensorTile(id, title, seqLen, seqLen, signedNorm = false, kind = TileKind.ATTENTION), LayerStacked {

    constructor(port: TensorPort, numHeads: Int, seqLen: Int, title: String = port.name) :
        this(listOf(port), numHeads, seqLen, title)

    init {
        require(stackLayers.isEmpty() || stackLayers.size == ports.size) {
            "Stack layers (${stackLayers.size}) must match ports (${ports.size})"
        }
    }

    private val history = FloatArray(ports.size * numHeads * rows * cols)
    private val lastVersions = LongArray(ports.size) { -1L }
    private var selected = 0

    override val shownLayer get() = stackLayers.getOrElse(selected) { -1 }

    @Synchronized
    override fun showLayer(layer: Int): Boolean {
        val index = stackLayers.indexOf(layer)
        if (index < 0) return false
        if (index != selected) {
            selected = index
            rebuildFromHistory()
        }
        return true
    }

    override fun reset() {
        super.reset()
        history.fill(0f)
        lastVersions.fill(-1L)
    }

    var selectedHead = 0
        set(value) {
            require(value in 0 until numHeads) { "Head $value out of range 0..${numHeads - 1}" }
            if (field != value) {
                field = value
                rebuildFromHistory()
            }
        }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        if (tokenIndex !in 0 until rows) return
        liveRow = tokenIndex
        val seen = minOf(tokenIndex + 1, cols)
        for ((s, port) in ports.withIndex()) {
            val tensor = port.tensor
            if (tensor.version == lastVersions[s]) continue
            lastVersions[s] = tensor.version
            for (head in 0 until numHeads) {
                val src = head * tensor.cols
                val dst = ((s * numHeads + head) * rows + tokenIndex) * cols
                for (j in 0 until seen) {
                    history[dst + j] = tensor.data.get(src + j)
                }
            }
        }
        System.arraycopy(
            history, ((selected * numHeads + selectedHead) * rows + tokenIndex) * cols,
            values, tokenIndex * cols, cols
        )
        touch()
    }

    @Synchronized
    private fun rebuildFromHistory() {
        System.arraycopy(history, (selected * numHeads + selectedHead) * rows * cols, values, 0, rows * cols)
        touch()
    }
}
