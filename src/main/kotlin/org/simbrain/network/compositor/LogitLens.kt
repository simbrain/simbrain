package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.matmul
import org.simbrain.network.tensor.op.TensorPort
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The logit lens: projects each source port (typically the residual stream after every layer)
 * through the model's final norm and unembedding, reading off the top predicted token per
 * layer — the prediction sharpening layer by layer as generation runs. The dirty sources are
 * normed into one batch and projected by a single vocab-sized sgemm, so the unembedding weight
 * streams through memory once per token rather than once per layer.
 *
 * Decode-shaped sources are 1 x dim vectors; full-sequence sources are seq x dim matrices with
 * [sourceRow] selecting the position the lens reads (the position about to predict). RMSNorm by
 * default; [meanCenter] plus [normBias] make it the LayerNorm a GPT-style teaching model uses.
 *
 * For the last layer's residual the lens is exactly the model's own output distribution, since it
 * applies the same norm and unembedding the model does.
 */
class LogitLens(
    private val embedWeight: FloatTensor,
    private val normWeight: FloatTensor,
    private val eps: Float,
    val sources: List<TensorPort>,
    private val normBias: FloatTensor? = null,
    private val meanCenter: Boolean = false,
) {

    class Reading {
        @Volatile
        var tokenId = 0
            internal set

        @Volatile
        var prob = 0f
            internal set
    }

    val readings = List(sources.size) { Reading() }

    /** Costs one vocab-sized sgemm per token; turn off to decode at full speed. */
    var enabled = true

    /**
     * When true, [refresh] snapshots the dirty rows and returns; a latest-wins worker computes
     * the projection and updates [readings] off the caller's thread. The pending slot holds one
     * snapshot — under load intermediate tokens are dropped, never queued, so the readings lag
     * the model by at most one in-flight pass.
     */
    var async = false

    /** Fired on the worker thread after an async pass lands, so hosts can schedule a repaint. */
    var onReadingsUpdated: (() -> Unit)? = null

    /** Which row of each source matrix the lens projects. Changing it re-reads every source. */
    var sourceRow = 0
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    private val hidden = embedWeight.cols
    private val batch = FloatTensor(sources.size, hidden)
    private val logits = FloatTensor(sources.size, embedWeight.rows)
    private val lastVersions = LongArray(sources.size) { -1L }

    private class Snapshot(val dirty: IntArray, val rows: FloatArray)

    private val pending = AtomicReference<Snapshot?>(null)
    private val draining = AtomicBoolean(false)

    fun reset() {
        lastVersions.fill(-1L)
        pending.set(null)
    }

    fun refresh() {
        if (!enabled) return
        var dirtyCount = 0
        val dirty = IntArray(sources.size)
        for ((i, source) in sources.withIndex()) {
            val version = source.tensor.version
            if (version == lastVersions[i]) continue
            lastVersions[i] = version
            dirty[dirtyCount++] = i
        }
        if (dirtyCount == 0) return
        val snapshot = Snapshot(dirty.copyOf(dirtyCount), snapshotRows(dirty, dirtyCount))
        if (async) {
            pending.set(snapshot)
            if (draining.compareAndSet(false, true)) worker.execute(::drain)
        } else {
            compute(snapshot)
        }
    }

    /** Copies the read row of each dirty source — the model mutates these tensors in place. */
    private fun snapshotRows(dirty: IntArray, dirtyCount: Int): FloatArray {
        val rows = FloatArray(dirtyCount * hidden)
        for (d in 0 until dirtyCount) {
            val tensor = sources[dirty[d]].tensor
            val base = sourceRow.coerceIn(0, tensor.rows - 1) * tensor.cols
            for (j in 0 until hidden) {
                rows[d * hidden + j] = tensor.data.get(base + j)
            }
        }
        return rows
    }

    private fun drain() {
        while (true) {
            val snapshot = pending.getAndSet(null) ?: break
            compute(snapshot)
            onReadingsUpdated?.invoke()
        }
        draining.set(false)
        if (pending.get() != null && draining.compareAndSet(false, true)) worker.execute(::drain)
    }

    private fun compute(snapshot: Snapshot) {
        val count = snapshot.dirty.size
        for (d in 0 until count) {
            norm(snapshot.rows, d * hidden, d)
        }
        batch.markMutated()
        matmul(batch, embedWeight, logits, transposeB = true, rowCount = count)
        for (d in 0 until count) {
            readOff(readings[snapshot.dirty[d]], d * logits.cols)
        }
    }

    private fun norm(src: FloatArray, srcBase: Int, batchRow: Int) {
        var mean = 0f
        if (meanCenter) {
            for (j in 0 until hidden) mean += src[srcBase + j]
            mean /= hidden
        }
        var sumSquares = 0f
        for (j in 0 until hidden) {
            val v = src[srcBase + j] - mean
            sumSquares += v * v
        }
        val inv = 1f / sqrt(sumSquares / hidden + eps)
        val dstBase = batchRow * hidden
        for (j in 0 until hidden) {
            val bias = normBias?.data?.get(j) ?: 0f
            batch.data.put(dstBase + j, (src[srcBase + j] - mean) * inv * normWeight.data.get(j) + bias)
        }
    }

    private fun readOff(reading: Reading, base: Int) {
        val vocab = logits.cols
        var best = 0
        var bestLogit = logits.data.get(base)
        for (j in 1 until vocab) {
            val l = logits.data.get(base + j)
            if (l > bestLogit) {
                bestLogit = l
                best = j
            }
        }
        var sumExp = 0f
        for (j in 0 until vocab) {
            sumExp += exp(logits.data.get(base + j) - bestLogit)
        }
        reading.tokenId = best
        reading.prob = 1f / sumExp
    }

    companion object {
        private val worker = Executors.newSingleThreadExecutor { r ->
            Thread(r, "logit-lens").apply { isDaemon = true }
        }
    }
}
