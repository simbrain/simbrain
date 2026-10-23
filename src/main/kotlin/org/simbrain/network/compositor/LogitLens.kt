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
        /** The predicted token, or -1 before the lens has read a computed pass. */
        @Volatile
        var tokenId = -1
            internal set

        @Volatile
        var prob = 0f
            internal set

        /** The source tensor version this reading was computed from; -1 before any. */
        @Volatile
        var sourceVersion = -1L
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

    private class Snapshot(val dirty: IntArray, val versions: LongArray, val rows: FloatArray)

    /** Buffers for [currentReading]'s one-source pass, apart from the batch the worker owns. */
    private val singleBatch by lazy { FloatTensor(1, hidden) }
    private val singleLogits by lazy { FloatTensor(1, embedWeight.rows) }

    private val pending = AtomicReference<Snapshot?>(null)
    private val draining = AtomicBoolean(false)

    fun reset() {
        lastVersions.fill(-1L)
        pending.set(null)
    }

    /**
     * Blanks every reading and treats the sources' current contents as already read, so
     * zeroed checkpoints aren't projected into a meaningless prediction on the next refresh.
     */
    fun clear() {
        pending.set(null)
        for ((i, source) in sources.withIndex()) lastVersions[i] = source.tensor.version
        for ((i, reading) in readings.withIndex()) {
            reading.tokenId = -1
            reading.prob = 0f
            reading.sourceVersion = lastVersions[i]
        }
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
        val versions = LongArray(dirtyCount) { lastVersions[dirty[it]] }
        val snapshot = Snapshot(dirty.copyOf(dirtyCount), versions, snapshotRows(dirty, dirtyCount))
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
        for (d in 0 until dirtyCount) copyRow(sources[dirty[d]].tensor, rows, d * hidden)
        return rows
    }

    private fun copyRow(tensor: FloatTensor, dst: FloatArray, dstBase: Int) {
        val base = sourceRow.coerceIn(0, tensor.rows - 1) * tensor.cols
        for (j in 0 until hidden) {
            dst[dstBase + j] = tensor.data.get(base + j)
        }
    }

    /**
     * The reading for source [index] as of that source's current contents, or null with the lens
     * off or nothing computed yet. An async pass can trail the model by a token, so a coupling
     * that reads the lens right after a step would pair the new state with the old prediction; a
     * stale reading is recomputed here, synchronously, for that one source.
     */
    fun currentReading(index: Int): Reading? {
        if (!enabled) return null
        val source = sources[index].tensor
        val stored = readings[index]
        val reading = if (stored.sourceVersion == source.version) stored else synchronized(singleBatch) {
            val version = source.version
            val row = FloatArray(hidden).also { copyRow(source, it, 0) }
            norm(row, 0, singleBatch, 0)
            singleBatch.markMutated()
            matmul(singleBatch, embedWeight, singleLogits, transposeB = true, rowCount = 1)
            Reading().also {
                readOff(it, singleLogits, 0)
                it.sourceVersion = version
            }
        }
        return reading.takeIf { it.tokenId >= 0 }
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
            norm(snapshot.rows, d * hidden, batch, d)
        }
        batch.markMutated()
        matmul(batch, embedWeight, logits, transposeB = true, rowCount = count)
        for (d in 0 until count) {
            val reading = readings[snapshot.dirty[d]]
            readOff(reading, logits, d * logits.cols)
            reading.sourceVersion = snapshot.versions[d]
        }
    }

    private fun norm(src: FloatArray, srcBase: Int, dst: FloatTensor, dstRow: Int) {
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
        val dstBase = dstRow * hidden
        for (j in 0 until hidden) {
            val bias = normBias?.data?.get(j) ?: 0f
            dst.data.put(dstBase + j, (src[srcBase + j] - mean) * inv * normWeight.data.get(j) + bias)
        }
    }

    private fun readOff(reading: Reading, logits: FloatTensor, base: Int) {
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
