package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.util.get
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.toMatrix

class SigmoidDiscreteArrayTest {

    val net = Network()
    var input1 = NeuronArray(2)
    var input2 = NeuronArray(2)
    val output = NeuronArray(2).apply {
        updateRule = SigmoidalRule()
    }

    var w13 = WeightMatrix(input1, output)
    var w23 = WeightMatrix(input2, output)

    init {
        net.addNetworkModels(input1, input2, output, w13, w23)
        input1.activations = doubleArrayOf(1.0, -1.0).toMatrix()
        input1.isClamped = true
        input2.activations = doubleArrayOf(-1.0, 1.0).toMatrix()
        input2.isClamped = true
        // Net input will be (1,-1) dot (-1,1) = 0
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

        val lowerBound = (output.updateRule as BoundedUpdateRule).lowerBound
        val upperBound = (output.updateRule as BoundedUpdateRule).upperBound

        val midpoint =  lowerBound + (upperBound - lowerBound) / 2

        net.update()
        assertEquals(midpoint, output.activations[0], 0.01, "Zero Input")
        assertEquals(midpoint, output.activations[1], 0.01, "Zero Input")

        output.biases = doubleArrayOf(100.0, 100.0).toMatrix()
        net.update()
        assertEquals(upperBound, output.activations[0], 0.01, "High bias")
        assertEquals(upperBound, output.activations[1], 0.01, "High bias")

        output.biases = doubleArrayOf(-100.0, -100.0).toMatrix()
        net.update()
        assertEquals(lowerBound, output.activations[0], 0.01, "Low bias")
        assertEquals(lowerBound, output.activations[1], 0.01, "Low bias")

        // Reset bias
        output.biases = doubleArrayOf(0.0, 0.0).toMatrix()

        input1.activations = doubleArrayOf(100.0, 100.0).toMatrix()
        net.update()
        assertEquals(upperBound, output.activations[0], 0.01, "High activation")
        assertEquals(upperBound, output.activations[1], 0.01, "High activation")

        input1.activations = doubleArrayOf(-100.0, -100.0).toMatrix()
        net.update()
        assertEquals(lowerBound, output.activations[0], 0.01, "Low activation")
        assertEquals(lowerBound, output.activations[1], 0.01, "Low activation")

    }

}