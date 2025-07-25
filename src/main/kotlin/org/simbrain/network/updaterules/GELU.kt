package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.applyFunction
import org.simbrain.util.copyFrom
import kotlin.math.*

/**
 * GELU activation function. See [https://paperswithcode.com/method/gelu]
 */
open class GELU : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), DifferentiableUpdateRule {

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        neuron.activation = gelu(neuron.input)
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        layer.activations.copyFrom(layer.inputs.applyFunction(::gelu))
    }

    fun clamp(x: Double, lower: Double, upper: Double): Double {
        return min(max(x, lower), upper)
    }

    fun gelu(input: Double): Double {
        val tanhInput = sqrt(2 / PI) * (input + (0.044715 * (input.pow(3))))
        val clampedTanhInput = clamp(tanhInput, -1000.0, 1000.0)

        return (input * 0.5) * (1 + tanh(clampedTanhInput))
    }

    override fun createMatrixData(size: Int): EmptyMatrixData {
        return EmptyMatrixData
    }

    override fun createScalarData(): EmptyScalarData {
        return EmptyScalarData
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): GELU {
        val gelu = GELU()
        return gelu
    }

    override fun getDerivative(value: Double): Double {
        val sqrtTerm = sqrt(2 / PI)
        val xCubed = value.pow(3)
        val tanhInput = sqrtTerm * (value + 0.044715 * xCubed)
        val clampedTanhInput = clamp(tanhInput, -1000.0, 1000.0)
        val tanhVal = tanh(clampedTanhInput)
        val sech2 = 1 - tanhVal.pow(2)
        val term1 = 0.5 * (1 + tanhVal)
        val term2 = 0.5 * value * sech2 * sqrtTerm * (1 + 3 * 0.044715 * value.pow(2))

        return term1 + term2
    }

    override val name: String
        get() = "GELU"

}