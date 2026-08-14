package org.simbrain.network.tensor.op

import org.simbrain.network.tensor.axpy
import org.simbrain.network.tensor.matmul
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Matrix-shaped (seq x dim) ops for full-sequence transformer passes, all VJP-complete so plans
 * built from them are trainable. Unlike the decode-shaped LFM2 ops, every op here recomputes the
 * whole sequence each forward — there are no caches and no position cursors.
 */

/**
 * Gathers one embedding row per position: out[r] = table[tokenIds[r]], with a negative id
 * producing a zero row (empty context positions and out-of-vocabulary tokens). When training
 * sample-by-sample, [tokenIds] must still hold the recorded sample's values at backward time.
 */
class SeqEmbedOp(name: String, val table: TensorPort, val out: TensorPort) : TensorOp(name) {

    var tokenIds = IntArray(out.tensor.rows) { -1 }

    override val inputs = listOf(table)
    override val outputs = listOf(out)

    override fun forward() {
        require(tokenIds.size == out.tensor.rows) {
            "tokenIds size ${tokenIds.size} != ${out.tensor.rows} positions"
        }
        val src = table.tensor.data
        val dst = out.tensor.data
        val dim = out.tensor.cols
        val vocab = table.tensor.rows
        for (r in 0 until out.tensor.rows) {
            val id = tokenIds[r]
            require(id < vocab) { "token id $id out of vocabulary ($vocab)" }
            for (c in 0 until dim) {
                dst.put(r * dim + c, if (id >= 0) src.get(id * dim + c) else 0f)
            }
        }
    }

    override val hasBackward get() = true

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gTable = grads.of(table.tensor).data
        val dim = out.tensor.cols
        for (r in 0 until out.tensor.rows) {
            val id = tokenIds[r]
            if (id < 0) continue
            for (c in 0 until dim) {
                gTable.put(id * dim + c, gTable.get(id * dim + c) + g.get(r * dim + c))
            }
        }
    }
}

/**
 * out = x . weight^T over a whole sequence, with [weight] in nn.Linear layout
 * (outFeatures x inFeatures) — the matrix-shaped counterpart of [LinearOp].
 */
class MatMulLinearOp(name: String, val weight: TensorPort, val x: TensorPort, val out: TensorPort) : TensorOp(name) {

    override val inputs = listOf(weight, x)
    override val outputs = listOf(out)

    override fun forward() = matmul(x.tensor, weight.tensor, out.tensor, transposeB = true)

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(weight.tensor, x.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor)
        matmul(g, weight.tensor, grads.of(x.tensor), beta = 1f)
        matmul(g, x.tensor, grads.of(weight.tensor), beta = 1f, transposeA = true)
    }
}

/** out = x + bias broadcast over rows; [bias] holds one value per column of [x]. */
class BiasOp(name: String, val x: TensorPort, val bias: TensorPort, val out: TensorPort) : TensorOp(name) {

    override val inputs = listOf(x, bias)
    override val outputs = listOf(out)

    override fun forward() {
        require(bias.tensor.size == x.tensor.cols) {
            "bias size ${bias.tensor.size} != ${x.tensor.cols} columns"
        }
        val src = x.tensor.data
        val b = bias.tensor.data
        val dst = out.tensor.data
        val cols = x.tensor.cols
        for (r in 0 until x.tensor.rows) {
            for (c in 0 until cols) {
                dst.put(r * cols + c, src.get(r * cols + c) + b.get(c))
            }
        }
    }

    override val hasBackward get() = true

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor)
        axpy(1f, g, grads.of(x.tensor))
        val gb = grads.of(bias.tensor).data
        val gd = g.data
        val cols = x.tensor.cols
        for (r in 0 until x.tensor.rows) {
            for (c in 0 until cols) {
                gb.put(c, gb.get(c) + gd.get(r * cols + c))
            }
        }
    }
}

/** out = max(0, x) elementwise. */
class ReLUOp(name: String, val x: TensorPort, val out: TensorPort) : TensorOp(name) {

    override val inputs = listOf(x)
    override val outputs = listOf(out)

    override fun forward() {
        val src = x.tensor.data
        val dst = out.tensor.data
        for (i in 0 until out.tensor.size) {
            dst.put(i, maxOf(0f, src.get(i)))
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(x.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gx = grads.of(x.tensor).data
        val src = x.tensor.data
        for (i in 0 until out.tensor.size) {
            if (src.get(i) > 0f) gx.put(i, gx.get(i) + g.get(i))
        }
    }
}

/**
 * Per-row layer normalization with learnable scale and shift: each row of [x] is mean-centered,
 * variance-normalized, then scaled by [gamma] and shifted by [beta] (one value per column).
 */
class LayerNormOp(
    name: String,
    val x: TensorPort,
    val gamma: TensorPort,
    val beta: TensorPort,
    val out: TensorPort,
    private val eps: Float = 1e-5f,
) : TensorOp(name) {

    override val inputs = listOf(x, gamma, beta)
    override val outputs = listOf(out)

    override fun forward() {
        require(gamma.tensor.size == x.tensor.cols && beta.tensor.size == x.tensor.cols) {
            "gamma/beta sizes ${gamma.tensor.size}/${beta.tensor.size} != ${x.tensor.cols} columns"
        }
        val src = x.tensor.data
        val g = gamma.tensor.data
        val b = beta.tensor.data
        val dst = out.tensor.data
        val cols = x.tensor.cols
        for (r in 0 until x.tensor.rows) {
            var mean = 0f
            for (c in 0 until cols) mean += src.get(r * cols + c)
            mean /= cols
            var variance = 0f
            for (c in 0 until cols) {
                val d = src.get(r * cols + c) - mean
                variance += d * d
            }
            variance /= cols
            val inv = 1f / sqrt(variance + eps)
            for (c in 0 until cols) {
                val xhat = (src.get(r * cols + c) - mean) * inv
                dst.put(r * cols + c, xhat * g.get(c) + b.get(c))
            }
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(x.tensor, gamma.tensor)

    override fun backward(grads: Gradients) {
        val gOut = grads.of(out.tensor).data
        val gx = grads.of(x.tensor).data
        val gGamma = grads.of(gamma.tensor).data
        val gBeta = grads.of(beta.tensor).data
        val src = x.tensor.data
        val g = gamma.tensor.data
        val cols = x.tensor.cols
        val dxhat = FloatArray(cols)
        val xhat = FloatArray(cols)
        for (r in 0 until x.tensor.rows) {
            var mean = 0f
            for (c in 0 until cols) mean += src.get(r * cols + c)
            mean /= cols
            var variance = 0f
            for (c in 0 until cols) {
                val d = src.get(r * cols + c) - mean
                variance += d * d
            }
            variance /= cols
            val inv = 1f / sqrt(variance + eps)
            var meanDxhat = 0f
            var meanDxhatXhat = 0f
            for (c in 0 until cols) {
                xhat[c] = (src.get(r * cols + c) - mean) * inv
                dxhat[c] = gOut.get(r * cols + c) * g.get(c)
                meanDxhat += dxhat[c]
                meanDxhatXhat += dxhat[c] * xhat[c]
            }
            meanDxhat /= cols
            meanDxhatXhat /= cols
            for (c in 0 until cols) {
                gx.put(r * cols + c, gx.get(r * cols + c) + inv * (dxhat[c] - meanDxhat - xhat[c] * meanDxhatXhat))
                gGamma.put(c, gGamma.get(c) + gOut.get(r * cols + c) * xhat[c])
                gBeta.put(c, gBeta.get(c) + gOut.get(r * cols + c))
            }
        }
    }
}

/**
 * Sequence-wide cross-entropy with per-row softmax fused in: writes every position's
 * distribution to [probs] (the visualizable prediction sequence) and the mean of
 * -ln(probs[r, targetIds[r]]) over supervised rows to scalar [loss]. A negative target id
 * excludes that row from the loss (empty context positions). When training sample-by-sample,
 * [targetIds] must still hold the recorded sample's values at backward time.
 */
class SeqSoftmaxCrossEntropyOp(
    name: String,
    val logits: TensorPort,
    val probs: TensorPort,
    val loss: TensorPort,
) : TensorOp(name) {

    var targetIds = IntArray(logits.tensor.rows) { -1 }

    override val inputs = listOf(logits)
    override val outputs = listOf(probs, loss)

    override fun forward() {
        require(targetIds.size == logits.tensor.rows) {
            "targetIds size ${targetIds.size} != ${logits.tensor.rows} positions"
        }
        val z = logits.tensor.data
        val p = probs.tensor.data
        val cols = logits.tensor.cols
        var totalLoss = 0f
        var supervised = 0
        for (r in 0 until logits.tensor.rows) {
            var max = Float.NEGATIVE_INFINITY
            for (c in 0 until cols) max = maxOf(max, z.get(r * cols + c))
            var sum = 0f
            for (c in 0 until cols) {
                val e = exp(z.get(r * cols + c) - max)
                p.put(r * cols + c, e)
                sum += e
            }
            val invSum = 1f / sum
            for (c in 0 until cols) p.put(r * cols + c, p.get(r * cols + c) * invSum)
            val target = targetIds[r]
            if (target >= 0) {
                require(target < cols) { "target id $target out of vocabulary ($cols)" }
                totalLoss += -ln(p.get(r * cols + target))
                supervised++
            }
        }
        loss.tensor.data.put(0, if (supervised > 0) totalLoss / supervised else 0f)
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(probs.tensor)

    override fun backward(grads: Gradients) {
        val gLoss = grads.of(loss.tensor).data.get(0)
        val gLogits = grads.of(logits.tensor).data
        val p = probs.tensor.data
        val cols = logits.tensor.cols
        val supervised = targetIds.count { it >= 0 }
        if (supervised == 0) return
        val scale = gLoss / supervised
        for (r in 0 until logits.tensor.rows) {
            val target = targetIds[r]
            if (target < 0) continue
            for (c in 0 until cols) {
                val indicator = if (c == target) 1f else 0f
                gLogits.put(r * cols + c, gLogits.get(r * cols + c) + scale * (p.get(r * cols + c) - indicator))
            }
        }
    }
}
