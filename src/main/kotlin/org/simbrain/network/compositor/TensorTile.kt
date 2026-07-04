package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.TensorPort
import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs

/** What a tile shows, driving its colormap and border styling. */
enum class TileKind { ACTIVATION, RESIDUAL, WEIGHT, ATTENTION, GRADIENT }

/**
 * One heatmap in the compositor's retained scene: a layout rect in scene coordinates, a value
 * buffer holding published data, and a raster the values are shaded into. The tiers of
 * invalidation map onto its methods: moving the rect is tier 1 (geometry only), [publish] is
 * tier 2 (copy fresh values, shade only the dirty rows), [markAllDirty] + [shadeDirty] with new
 * colors is tier 3 (reshade from the value buffer without touching data), and zoom never calls
 * back in at all (the node's image cache rescales).
 *
 * [publish] is called from the compute thread and shading from the EDT; both synchronize on the
 * tile, which is cheap at row granularity.
 */
abstract class TensorTile(
    val id: String,
    val title: String,
    val rows: Int,
    val cols: Int,
    private val signedNorm: Boolean,
    val kind: TileKind = TileKind.ACTIVATION,
) {

    var x = 0.0
    var y = 0.0
    var width = cols.toDouble()
    var height = rows.toDouble()

    /** Published data, row-major [rows] x [cols]. Read for tooltips and probes; written by [publish]. */
    val values = FloatArray(rows * cols)

    val image: BufferedImage = BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB)
    private val pixels = (image.raster.dataBuffer as DataBufferInt).data

    protected var absMax = 0f
        private set

    private var dirtyFrom = -1
    private var dirtyTo = -1
    private var fullReshade = true

    val isDirty get() = fullReshade || dirtyFrom >= 0

    /** Copies this token's fresh values out of the source tensor and marks the touched rows dirty. */
    abstract fun publish(tokenIndex: Int)

    /** Clears published history for a fresh generation run. */
    @Synchronized
    open fun reset() {
        values.fill(0f)
        absMax = 0f
        markAllDirty()
    }

    protected fun markRowsDirty(from: Int, to: Int = from) {
        dirtyFrom = if (dirtyFrom < 0) from else minOf(dirtyFrom, from)
        dirtyTo = maxOf(dirtyTo, to)
    }

    fun markAllDirty() {
        fullReshade = true
    }

    /** Forgets the signed-normalization scale — for view switches to differently-scaled data. */
    protected fun resetScale() {
        absMax = 0f
        markAllDirty()
    }

    /**
     * Grows the signed-normalization scale. Growth invalidates every already-shaded pixel, so the
     * whole tile reshades; the scale never shrinks mid-run, keeping steady-state cost at one row.
     */
    protected fun growAbsMax(candidate: Float) {
        if (candidate > absMax) {
            absMax = candidate
            fullReshade = true
        }
    }

    /** Shades dirty rows from [values] into [image] and clears the dirty state. */
    @Synchronized
    fun shadeDirty(neg: Color, mid: Color, pos: Color) {
        if (!isDirty) return
        val from = if (fullReshade) 0 else dirtyFrom
        val to = if (fullReshade) rows - 1 else dirtyTo
        val scale = if (signedNorm) (if (absMax > 0f) 1f / absMax else 0f) else 1f
        for (i in from * cols..(to * cols + cols - 1)) {
            pixels[i] = (values[i] * scale).toSimbrainColor(neg, mid, pos)
        }
        fullReshade = false
        dirtyFrom = -1
        dirtyTo = -1
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
 */
class VectorHistoryTile(
    val port: TensorPort,
    rows: Int,
    title: String = port.name,
) : TensorTile(port.name, title, rows, port.tensor.size, signedNorm = true, kind = TileKind.RESIDUAL) {

    private var lastVersion = -1L
    private val magnitudes = FloatArray(cols)

    override fun reset() {
        super.reset()
        lastVersion = -1L
    }

    @Synchronized
    override fun publish(tokenIndex: Int) {
        if (tokenIndex !in 0 until rows) return
        val tensor = port.tensor
        if (tensor.version == lastVersion) return
        lastVersion = tensor.version
        val base = tokenIndex * cols
        for (i in 0 until cols) {
            val v = tensor.data.get(i)
            values[base + i] = v
            magnitudes[i] = abs(v)
        }
        magnitudes.sort()
        val quantile = magnitudes[(cols * 995 / 1000).coerceAtMost(cols - 1)]
        growAbsMax(if (quantile > 0f) quantile else magnitudes[cols - 1])
        markRowsDirty(tokenIndex)
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
    val tensor: FloatTensor,
    kind: TileKind = TileKind.ACTIVATION,
    signedNorm: Boolean = true,
    private val quantileNorm: Boolean = false,
    private val versionGated: Boolean = true,
    private val displayTransposed: Boolean = false,
) : TensorTile(
    id, title,
    if (displayTransposed) tensor.cols else tensor.rows,
    if (displayTransposed) tensor.rows else tensor.cols,
    signedNorm, kind,
) {

    constructor(
        port: TensorPort,
        kind: TileKind = TileKind.ACTIVATION,
        title: String = port.name,
        signedNorm: Boolean = true,
        quantileNorm: Boolean = false,
        displayTransposed: Boolean = false,
    ) : this(port.name, title, port.tensor, kind, signedNorm, quantileNorm, true, displayTransposed)

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
        markAllDirty()
    }

    private fun normalizationScale(): Float {
        if (!quantileNorm) {
            var max = 0f
            for (v in values) max = maxOf(max, abs(v))
            return max
        }
        val magnitudes = FloatArray(values.size) { abs(values[it]) }
        magnitudes.sort()
        val quantile = magnitudes[(magnitudes.size * 995 / 1000).coerceAtMost(magnitudes.size - 1)]
        return if (quantile > 0f) quantile else magnitudes[magnitudes.size - 1]
    }
}

/**
 * A stacked rank-3 tile: the source tensor holds [slices] stacked row blocks (slice s occupying
 * rows [s*rows, (s+1)*rows)) — per-head attention maps, per-head projections — and the tile
 * renders one slice at a time. Every slice is retained in a cube on publish, so [selectedSlice]
 * flips instantly by reshading from memory with zero recompute. Full-pass semantics like
 * [MatrixTile]: publish is version-gated and ignores the token index.
 */
class DeckTile(
    id: String,
    title: String,
    val tensor: FloatTensor,
    val slices: Int,
    kind: TileKind = TileKind.ATTENTION,
    signedNorm: Boolean = false,
) : TensorTile(id, title, tensor.rows / slices, tensor.cols, signedNorm, kind) {

    constructor(
        port: TensorPort,
        slices: Int,
        kind: TileKind = TileKind.ATTENTION,
        title: String = port.name,
        signedNorm: Boolean = false,
    ) : this(port.name, title, port.tensor, slices, kind, signedNorm)

    init {
        require(tensor.rows % slices == 0) { "${tensor.rows} rows not divisible into $slices slices" }
    }

    private val cube = FloatArray(tensor.rows * tensor.cols)
    private var lastVersion = -1L

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
        for (i in cube.indices) cube[i] = tensor.data.get(i)
        var max = 0f
        for (v in cube) max = maxOf(max, abs(v))
        growAbsMax(max)
        System.arraycopy(cube, selectedSlice * rows * cols, values, 0, rows * cols)
        markAllDirty()
    }

    @Synchronized
    private fun rebuildFromCube() {
        System.arraycopy(cube, selectedSlice * rows * cols, values, 0, rows * cols)
        markAllDirty()
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
    val port: TensorPort,
    val numHeads: Int,
    seqLen: Int,
    title: String = port.name,
) : TensorTile(port.name, title, seqLen, seqLen, signedNorm = false, kind = TileKind.ATTENTION) {

    private val history = FloatArray(numHeads * rows * cols)
    private var lastVersion = -1L

    override fun reset() {
        super.reset()
        history.fill(0f)
        lastVersion = -1L
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
        val tensor = port.tensor
        if (tensor.version == lastVersion) return
        lastVersion = tensor.version
        val seen = minOf(tokenIndex + 1, cols)
        for (head in 0 until numHeads) {
            val src = head * tensor.cols
            val dst = (head * rows + tokenIndex) * cols
            for (j in 0 until seen) {
                history[dst + j] = tensor.data.get(src + j)
            }
        }
        System.arraycopy(history, (selectedHead * rows + tokenIndex) * cols, values, tokenIndex * cols, cols)
        markRowsDirty(tokenIndex)
    }

    @Synchronized
    private fun rebuildFromHistory() {
        System.arraycopy(history, selectedHead * rows * cols, values, 0, rows * cols)
        markAllDirty()
    }
}
