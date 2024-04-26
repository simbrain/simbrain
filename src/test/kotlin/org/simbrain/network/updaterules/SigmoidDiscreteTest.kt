package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.math.SigmoidFunctionEnum

class SigmoidDiscreteTest {

    val net = Network()
    var input1 = Neuron()
    var input2 = Neuron()
    val output = Neuron().apply {
        updateRule = SigmoidalRule()
    }

    var w13 = Synapse(input1, output)
    var w23 = Synapse(input2, output)

    init {
        net.addNetworkModels(input1, input2, output, w13, w23)
        input1.activation = 1.0
        input1.clamped = true

        input2.activation = 1.0
        input2.clamped = true

        w13.strength = 1.0
        w23.strength = -1.0

        // Net input will be 0
    }

    @Test
    fun `test logistic lower bound 0 upper bound 1`() {
        (output.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.LOGISTIC
        (output.updateRule as SigmoidalRule).lowerBound = 0.0
        sigmoidTests()
    }

    @Test
    fun `test logistic lower bound -1 upper bound 1`() {
        (output.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.LOGISTIC
        (output.updateRule as SigmoidalRule).lowerBound = -1.0
        sigmoidTests()
     }

    @Test
    fun `test atan lower bound 0 `() {
        (output.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.ARCTAN
        (output.updateRule as SigmoidalRule).lowerBound = 0.0
        sigmoidTests()
    }

    @Test
    fun `test atan lower bound -1 `() {
        (output.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.ARCTAN
        (output.updateRule as SigmoidalRule).lowerBound = -1.0
        sigmoidTests()
    }

    @Test
    fun `test tanh lower bound 0 `() {
        (output.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.TANH
        (output.updateRule as SigmoidalRule).lowerBound = 0.0
        sigmoidTests()
    }

    @Test
    fun `test tanh lower bound -1 `() {
        (output.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.TANH
        (output.updateRule as SigmoidalRule).lowerBound = -1.0
        sigmoidTests()
    }

    /**
     * Change biases and inputs and ensure correct outputs.
     */
    private fun sigmoidTests() {

        val midpoint =  output.lowerBound + (output.upperBound - output.lowerBound) / 2

        net.update()
        assertEquals(midpoint, output.activation, 0.01, "Zero Input")

        (output.dataHolder as BiasedScalarData).bias = 100.0
        net.update()
        assertEquals(output.upperBound, output.activation, 0.01, "High bias")

        (output.dataHolder as BiasedScalarData).bias = -100.0
        net.update()
        assertEquals(output.lowerBound, output.activation, 0.01, "Low bias")

        // Reset bias
        (output.dataHolder as BiasedScalarData).bias = 0.0

        input1.activation = 100.0
        net.update()
        assertEquals(output.upperBound, output.activation, 0.01, "High activation")

        input1.activation = -100.0
        net.update()
        assertEquals(output.lowerBound, output.activation, 0.01, "Low activation")

    }

}