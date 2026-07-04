package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.matvec
import org.simbrain.network.tensor.op.TensorPort
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The logit lens: projects each source port (typically the residual stream after every layer)
 * through the model's final norm and tied unembedding, reading off the top predicted token per
 * layer — the prediction sharpening layer by layer as generation runs. With tied embeddings this
 * is one norm + one vocab-sized matvec per layer, computed on the compute thread at publish time.
 *
 * For the last layer's residual the lens is exactly the model's own output distribution, since it
 * applies the same norm and unembedding the model does.
 */
class LogitLens(
    private val embedWeight: FloatTensor,
    private val normWeight: FloatTensor,
    private val eps: Float,
    val sources: List<TensorPort>,
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
            rmsNorm(tensor)
            matvec(embedWeight, normed, logits)
            readOff(readings[i])
        }
    }

    private fun rmsNorm(x: FloatTensor) {
        val n = x.size
        var sumSquares = 0f
        for (j in 0 until n) {
            val v = x.data.get(j)
            sumSquares += v * v
        }
        val inv = 1f / sqrt(sumSquares / n + eps)
        for (j in 0 until n) {
            normed.data.put(j, x.data.get(j) * inv * normWeight.data.get(j))
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
