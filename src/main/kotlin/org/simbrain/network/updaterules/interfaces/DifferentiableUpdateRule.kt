package org.simbrain.network.updaterules.interfaces

import org.simbrain.util.applyFunction
import smile.math.matrix.Matrix

/**
 * Indicates that an update rule is differentiable, and has a getDerivative
 * function. Used by backprop.
 *
 * @author jyoshimi
 */
interface DifferentiableUpdateRule {
    /**
     * The derivative of the activation function.
     *
     * @param value the value being sent through the neuron's derivative
     * @return the derivative of the neuron's activation function with respect
     * to val.
     */
    fun getDerivative(value: Double): Double

    /**
     * Array based derivative. By default forwards to scalar derivative.
     */
    fun getDerivative(input: Matrix): Matrix {
        return input.applyFunction(::getDerivative)
    }
}
