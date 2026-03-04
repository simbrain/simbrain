package org.simbrain.network.trainers

import kotlin.math.sqrt

/**
 * Adam optimizer operating on DoubleArrays, keyed by string identifiers.
 * Each parameter group (e.g., "conv1_kernels", "dense1_weights") has its own
 * first and second moment arrays.
 */
class DoubleArrayAdam(
    var learningRate: Double = 0.001,
    var beta1: Double = 0.9,
    var beta2: Double = 0.999,
    var epsilon: Double = 1e-8
) {
    private val m = mutableMapOf<String, DoubleArray>()
    private val v = mutableMapOf<String, DoubleArray>()
    var timestep = 0
        private set

    /**
     * Apply one Adam update step to [params] using [grad].
     * Both arrays are modified in-place (params updated, grad unchanged).
     *
     * @param key    unique identifier for this parameter group
     * @param params the parameter array to update in-place
     * @param grad   the gradient array (same length as params)
     */
    fun update(key: String, params: DoubleArray, grad: DoubleArray) {
        val size = params.size
        val mArr = m.getOrPut(key) { DoubleArray(size) }
        val vArr = v.getOrPut(key) { DoubleArray(size) }

        // Increment timestep on first call per batch (caller increments once, or we track per-key)
        // We use a shared timestep that the caller increments via step()

        val bc1 = 1.0 - Math.pow(beta1, timestep.toDouble())
        val bc2 = 1.0 - Math.pow(beta2, timestep.toDouble())
        val lr = learningRate * sqrt(bc2) / bc1

        for (i in 0 until size) {
            val g = grad[i]
            mArr[i] = beta1 * mArr[i] + (1.0 - beta1) * g
            vArr[i] = beta2 * vArr[i] + (1.0 - beta2) * g * g
            params[i] -= lr * mArr[i] / (sqrt(vArr[i]) + epsilon)
        }
    }

    /**
     * Increment the global timestep. Call once per training iteration (before update calls).
     */
    fun step() {
        timestep++
    }

    /**
     * Reset all moment estimates and timestep.
     */
    fun reset() {
        m.clear()
        v.clear()
        timestep = 0
    }
}
