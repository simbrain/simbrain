package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.matvec
import org.simbrain.network.tensor.op.TensorPort
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The logit lens: projects each source port (typically the residual stream after every layer)
 * through the model's final norm and unembedding, reading off the top predicted token per
 * layer — the prediction sharpening layer by layer as generation runs. One norm + one
 * vocab-sized matvec per layer, computed on the compute thread at publish time.
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
        var tokenId = 0
            internal set
        var prob = 0f
            internal set
    }

    val readings = List(sources.size) { Reading() }

    /** Lens GEMVs cost ~3.5 ms per layer; turn off to decode at full speed. */
    var enabled = true

    /** Which row of each source matrix the lens projects. Changing it re-reads every source. */
    var sourceRow = 0
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    private val normed = FloatTensor(1, embedWeight.cols)
    private val logits = FloatTensor(1, embedWeight.rows)
    private val lastVersions = LongArray(sources.size) { -1L }

    fun reset() = lastVersions.fill(-1L)

    fun refresh() {
        if (!enabled) return
        for ((i, source) in sources.withIndex()) {
            val tensor = source.tensor
            if (tensor.version == lastVersions[i]) continue
            lastVersions[i] = tensor.version
            norm(tensor)
            matvec(embedWeight, normed, logits)
            readOff(readings[i])
        }
    }

    private fun norm(x: FloatTensor) {
        val n = x.cols
        val base = sourceRow.coerceIn(0, x.rows - 1) * n
        var mean = 0f
        if (meanCenter) {
            for (j in 0 until n) mean += x.data.get(base + j)
            mean /= n
        }
        var sumSquares = 0f
        for (j in 0 until n) {
            val v = x.data.get(base + j) - mean
            sumSquares += v * v
        }
        val inv = 1f / sqrt(sumSquares / n + eps)
        for (j in 0 until n) {
            val bias = normBias?.data?.get(j) ?: 0f
            normed.data.put(j, (x.data.get(base + j) - mean) * inv * normWeight.data.get(j) + bias)
        }
        normed.markMutated()
    }

    private fun readOff(reading: Reading) {
        var best = 0
        var bestLogit = logits.data.get(0)
        for (j in 1 until logits.size) {
            val l = logits.data.get(j)
            if (l > bestLogit) {
                bestLogit = l
                best = j
            }
        }
        var sumExp = 0f
        for (j in 0 until logits.size) {
            sumExp += exp(logits.data.get(j) - bestLogit)
        }
        reading.tokenId = best
        reading.prob = 1f / sumExp
    }
}
