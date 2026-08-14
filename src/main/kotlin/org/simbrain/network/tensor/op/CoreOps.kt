package org.simbrain.network.tensor.op

import org.simbrain.network.tensor.axpy
import org.simbrain.network.tensor.ger
import org.simbrain.network.tensor.matvec
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * out = weight . x, with [weight] in nn.Linear layout (outFeatures x inFeatures) and [x]/[out]
 * vector-shaped. No bias (the LFM2 family has none; add a BiasOp when a teaching block needs one).
 */
class LinearOp(name: String, val weight: TensorPort, val x: TensorPort, val out: TensorPort) : TensorOp(name) {

    override val inputs = listOf(weight, x)
    override val outputs = listOf(out)

    override fun forward() = matvec(weight.tensor, x.tensor, out.tensor)

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(weight.tensor, x.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor)
        matvec(weight.tensor, g, grads.of(x.tensor), beta = 1f, transposeA = true)
        ger(g, x.tensor, grads.of(weight.tensor))
    }
}

/** out = a + b (residual add). */
class AddOp(name: String, val a: TensorPort, val b: TensorPort, val out: TensorPort) : TensorOp(name) {

    override val inputs = listOf(a, b)
    override val outputs = listOf(out)

    override fun forward() {
        out.tensor.copyFrom(a.tensor)
        axpy(1f, b.tensor, out.tensor)
    }

    override val hasBackward get() = true

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor)
        axpy(1f, g, grads.of(a.tensor))
        axpy(1f, g, grads.of(b.tensor))
    }
}

/** out = silu(gate) * up — the SwiGLU nonlinearity, fused so the plan sees one activation port. */
class SiluGateOp(name: String, val gate: TensorPort, val up: TensorPort, val out: TensorPort) : TensorOp(name) {

    override val inputs = listOf(gate, up)
    override val outputs = listOf(out)

    override fun forward() {
        val g = gate.tensor.data
        val u = up.tensor.data
        val o = out.tensor.data
        for (i in 0 until out.tensor.size) {
            val a = g.get(i)
            o.put(i, a / (1f + exp(-a)) * u.get(i))
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(gate.tensor, up.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gGate = grads.of(gate.tensor).data
        val gUp = grads.of(up.tensor).data
        val a = gate.tensor.data
        val u = up.tensor.data
        for (i in 0 until out.tensor.size) {
            val x = a.get(i)
            val s = 1f / (1f + exp(-x))
            val silu = x * s
            gGate.put(i, gGate.get(i) + g.get(i) * u.get(i) * (s + silu * (1f - s)))
            gUp.put(i, gUp.get(i) + g.get(i) * silu)
        }
    }
}

/** out = x * rsqrt(mean(x^2) + eps) * weight, over the whole vector (RMSNorm, f32). */
class RmsNormOp(
    name: String,
    val x: TensorPort,
    val weight: TensorPort,
    val out: TensorPort,
    private val eps: Float,
) : TensorOp(name) {

    override val inputs = listOf(x, weight)
    override val outputs = listOf(out)

    override fun forward() {
        val src = x.tensor.data
        val w = weight.tensor.data
        val dst = out.tensor.data
        val n = out.tensor.size
        var sumSq = 0f
        for (i in 0 until n) {
            val a = src.get(i)
            sumSq += a * a
        }
        val inv = 1f / sqrt(sumSq / n + eps)
        for (i in 0 until n) {
            dst.put(i, src.get(i) * inv * w.get(i))
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(x.tensor, weight.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gx = grads.of(x.tensor).data
        val gw = grads.of(weight.tensor).data
        val src = x.tensor.data
        val w = weight.tensor.data
        val n = out.tensor.size
        var sumSq = 0f
        for (i in 0 until n) {
            val a = src.get(i)
            sumSq += a * a
        }
        val inv = 1f / sqrt(sumSq / n + eps)
        var gDotWx = 0f
        for (i in 0 until n) {
            gDotWx += g.get(i) * w.get(i) * src.get(i)
        }
        val invCubedOverN = inv * inv * inv / n
        for (i in 0 until n) {
            gx.put(i, gx.get(i) + g.get(i) * w.get(i) * inv - src.get(i) * invCubedOverN * gDotWx)
            gw.put(i, gw.get(i) + g.get(i) * src.get(i) * inv)
        }
    }
}

/**
 * Cross-entropy against [targetIndex] with the softmax fused in: writes the full distribution
 * to [probs] (a first-class port — it's the visualizable prediction) and -ln(probs[target]) to
 * scalar [loss]. When training sample-by-sample, [targetIndex] must still hold the recorded
 * sample's value at backward time.
 */
class SoftmaxCrossEntropyOp(
    name: String,
    val logits: TensorPort,
    val probs: TensorPort,
    val loss: TensorPort,
) : TensorOp(name) {

    var targetIndex = 0

    override val inputs = listOf(logits)
    override val outputs = listOf(probs, loss)

    override fun forward() {
        val z = logits.tensor.data
        val p = probs.tensor.data
        val n = logits.tensor.size
        var max = Float.NEGATIVE_INFINITY
        for (i in 0 until n) max = maxOf(max, z.get(i))
        var sum = 0f
        for (i in 0 until n) {
            val e = exp(z.get(i) - max)
            p.put(i, e)
            sum += e
        }
        val invSum = 1f / sum
        for (i in 0 until n) p.put(i, p.get(i) * invSum)
        loss.tensor.data.put(0, -ln(p.get(targetIndex)))
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(probs.tensor)

    override fun backward(grads: Gradients) {
        val gLoss = grads.of(loss.tensor).data.get(0)
        val gLogits = grads.of(logits.tensor).data
        val p = probs.tensor.data
        for (i in 0 until logits.tensor.size) {
            val indicator = if (i == targetIndex) 1f else 0f
            gLogits.put(i, gLogits.get(i) + gLoss * (p.get(i) - indicator))
        }
    }
}
