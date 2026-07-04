package org.simbrain.network.compositor

import org.simbrain.network.tensor.op.TensorPort
import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs

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
) : TensorTile(port.name, title, rows, port.tensor.size, signedNorm = true) {

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
) : TensorTile(port.name, title, seqLen, seqLen, signedNorm = false) {

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
