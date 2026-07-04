package org.simbrain.network.tensor.op

import org.simbrain.network.tensor.FloatTensor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Adam over [FloatTensor] parameters, keyed by stable parameter names — names, not object
 * identity, key the moment state (the DoubleArrayAdam pattern, better for serialization and
 * debugging). Call [step] once per training iteration, then [update] per parameter.
 */
class TensorAdam(
    var learningRate: Float = 1e-3f,
    var beta1: Float = 0.9f,
    var beta2: Float = 0.999f,
    var epsilon: Float = 1e-8f,
) {

    private val m = HashMap<String, FloatArray>()
    private val v = HashMap<String, FloatArray>()

    var timestep = 0
        private set

    fun step() {
        timestep++
    }

    fun update(key: String, params: FloatTensor, grad: FloatTensor) {
        require(params.size == grad.size) { "Adam $key: params ${params.size} != grad ${grad.size}" }
        check(timestep > 0) { "Call step() before update()" }
        val size = params.size
        val mArr = m.getOrPut(key) { FloatArray(size) }
        val vArr = v.getOrPut(key) { FloatArray(size) }
        val bc1 = 1.0 - beta1.toDouble().pow(timestep)
        val bc2 = 1.0 - beta2.toDouble().pow(timestep)
        val lr = (learningRate * sqrt(bc2) / bc1).toFloat()
        val p = params.data
        val g = grad.data
        for (i in 0 until size) {
            val gi = g.get(i)
            mArr[i] = beta1 * mArr[i] + (1f - beta1) * gi
            vArr[i] = beta2 * vArr[i] + (1f - beta2) * gi * gi
            p.put(i, p.get(i) - lr * mArr[i] / (sqrt(vArr[i]) + epsilon))
        }
        params.markMutated()
    }

    fun reset() {
        m.clear()
        v.clear()
        timestep = 0
    }
}
