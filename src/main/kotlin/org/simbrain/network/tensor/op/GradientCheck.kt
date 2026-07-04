package org.simbrain.network.tensor.op

import org.simbrain.network.tensor.FloatTensor
import kotlin.math.abs
import kotlin.random.Random

/**
 * Finite-difference gradient checking for op VJPs: central differences on sampled coordinates
 * of a tensor, compared against the analytic gradient a tape backward produced. f32 forward
 * passes put the practical noise floor around 1e-2 relative — a VJP bug shows up orders of
 * magnitude above that, not near it.
 */
object GradientCheck {

    /**
     * Perturbs up to [samples] coordinates of [tensor], re-evaluating [loss] (a full, tape-free
     * forward returning the scalar loss) around each, and returns the worst relative error
     * against [analyticGrad]. [tensor] is restored after each probe.
     */
    fun maxRelativeError(
        tensor: FloatTensor,
        analyticGrad: FloatTensor,
        samples: Int = 32,
        seed: Long = 42L,
        loss: () -> Float,
    ): Float {
        val n = tensor.size
        val indices = if (n <= samples) {
            (0 until n).toList()
        } else {
            (0 until n).shuffled(Random(seed)).take(samples)
        }
        var worst = 0f
        for (idx in indices) {
            val r = idx / tensor.cols
            val c = idx % tensor.cols
            val v = tensor[r, c]
            val eps = maxOf(1e-3f, abs(v) * 1e-2f)
            tensor[r, c] = v + eps
            val fPlus = loss()
            tensor[r, c] = v - eps
            val fMinus = loss()
            tensor[r, c] = v
            val numeric = (fPlus - fMinus) / (2f * eps)
            val analytic = analyticGrad[r, c]
            val rel = abs(numeric - analytic) / maxOf(abs(numeric) + abs(analytic), 1e-2f)
            worst = maxOf(worst, rel)
        }
        return worst
    }
}
