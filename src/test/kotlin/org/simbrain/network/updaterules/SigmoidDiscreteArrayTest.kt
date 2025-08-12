package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.util.get
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.toColumnVector

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
        net.addNetworkModelsAsync(input1, input2, output, w13, w23)
        input1.activations = doubleArrayOf(1.0, -1.0).toColumnVector()
        input1.isClamped = true
        input2.activations = doubleArrayOf(-1.0, 1.0).toColumnVector()
        input2.isClamped = true
        // Net input will be (1,-1) dot (-1,1) = 0
    }

    // For scalar vs array comparison tests
    val scalarNet = Network()
    val scalarNeuron = Neuron().apply {
        updateRule = SigmoidalRule()
    }
    val arrayNeuron = NeuronArray(1).apply {
        updateRule = SigmoidalRule()
    }

    init {
        scalarNet.addNetworkModelsAsync(scalarNeuron, arrayNeuron)
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

        output.biases = doubleArrayOf(100.0, 100.0).toColumnVector()
        net.update()
        assertEquals(upperBound, output.activations[0], 0.01, "High bias")
        assertEquals(upperBound, output.activations[1], 0.01, "High bias")

        output.biases = doubleArrayOf(-100.0, -100.0).toColumnVector()
        net.update()
        assertEquals(lowerBound, output.activations[0], 0.01, "Low bias")
        assertEquals(lowerBound, output.activations[1], 0.01, "Low bias")

        // Reset bias
        output.biases = doubleArrayOf(0.0, 0.0).toColumnVector()

        input1.activations = doubleArrayOf(100.0, 100.0).toColumnVector()
        net.update()
        assertEquals(upperBound, output.activations[0], 0.01, "High activation")
        assertEquals(upperBound, output.activations[1], 0.01, "High activation")

        input1.activations = doubleArrayOf(-100.0, -100.0).toColumnVector()
        net.update()
        assertEquals(lowerBound, output.activations[0], 0.01, "Low activation")
        assertEquals(lowerBound, output.activations[1], 0.01, "Low activation")

    }

    /**
     * Test that compares scalar and array versions of the sigmoid function
     * to ensure they produce the same results.
     */
    @Test
    fun `test scalar vs array logistic`() {
        // Configure both neurons with the same settings
        (scalarNeuron.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.LOGISTIC
        (scalarNeuron.updateRule as SigmoidalRule).lowerBound = 0.0
        (scalarNeuron.updateRule as SigmoidalRule).upperBound = 1.0
        (scalarNeuron.updateRule as SigmoidalRule).slope = 1.0

        (arrayNeuron.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.LOGISTIC
        (arrayNeuron.updateRule as SigmoidalRule).lowerBound = 0.0
        (arrayNeuron.updateRule as SigmoidalRule).upperBound = 1.0
        (arrayNeuron.updateRule as SigmoidalRule).slope = 1.0

        compareScalarAndArray()
    }

    @Test
    fun `test scalar vs array arctan`() {
        // Configure both neurons with the same settings
        (scalarNeuron.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.ARCTAN
        (scalarNeuron.updateRule as SigmoidalRule).lowerBound = -1.0
        (scalarNeuron.updateRule as SigmoidalRule).upperBound = 1.0
        (scalarNeuron.updateRule as SigmoidalRule).slope = 1.0

        (arrayNeuron.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.ARCTAN
        (arrayNeuron.updateRule as SigmoidalRule).lowerBound = -1.0
        (arrayNeuron.updateRule as SigmoidalRule).upperBound = 1.0
        (arrayNeuron.updateRule as SigmoidalRule).slope = 1.0

        compareScalarAndArray()
    }

    @Test
    fun `test scalar vs array tanh`() {
        // Configure both neurons with the same settings
        (scalarNeuron.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.TANH
        (scalarNeuron.updateRule as SigmoidalRule).lowerBound = -1.0
        (scalarNeuron.updateRule as SigmoidalRule).upperBound = 1.0
        (scalarNeuron.updateRule as SigmoidalRule).slope = 1.0

        (arrayNeuron.updateRule as SigmoidalRule).type = SigmoidFunctionEnum.TANH
        (arrayNeuron.updateRule as SigmoidalRule).lowerBound = -1.0
        (arrayNeuron.updateRule as SigmoidalRule).upperBound = 1.0
        (arrayNeuron.updateRule as SigmoidalRule).slope = 1.0

        compareScalarAndArray()
    }

    /**
     * Helper method to compare scalar and array implementations with various inputs.
     */
    private fun compareScalarAndArray() {
        // Test with zero input
        scalarNeuron.clear()
        arrayNeuron.clear()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "Zero input")

        // Test with bias
        scalarNeuron.bias = 2.0
        arrayNeuron.biases = doubleArrayOf(2.0).toColumnVector()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "With bias")

        // Test with high bias
        scalarNeuron.bias = 100.0
        arrayNeuron.biases = doubleArrayOf(100.0).toColumnVector()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "High bias")

        // Test with low bias
        scalarNeuron.bias = -100.0
        arrayNeuron.biases = doubleArrayOf(-100.0).toColumnVector()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "Low bias")

        // Reset bias
        scalarNeuron.bias = 0.0
        arrayNeuron.biases = doubleArrayOf(0.0).toColumnVector()

        // Create input sources
        val inputNeuron = Neuron()
        val inputArray = NeuronArray(1)
        scalarNet.addNetworkModelsAsync(inputNeuron, inputArray)

        val scalarSynapse = Synapse(inputNeuron, scalarNeuron)
        scalarSynapse.strength = 1.0
        val arrayWeight = WeightMatrix(inputArray, arrayNeuron)
        arrayWeight.setWeights(doubleArrayOf(1.0))

        scalarNet.addNetworkModelsAsync(scalarSynapse, arrayWeight)

        // Test with positive activation
        inputNeuron.activation = 5.0
        inputArray.activations = doubleArrayOf(5.0).toColumnVector()
        inputNeuron.clamped = true
        inputArray.isClamped = true
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "Positive activation")

        // Test with negative activation
        inputNeuron.activation = -5.0
        inputArray.activations = doubleArrayOf(-5.0).toColumnVector()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "Negative activation")

        // Test with very large positive activation
        inputNeuron.activation = 100.0
        inputArray.activations = doubleArrayOf(100.0).toColumnVector()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "Large positive activation")

        // Test with very large negative activation
        inputNeuron.activation = -100.0
        inputArray.activations = doubleArrayOf(-100.0).toColumnVector()
        scalarNet.update()
        assertEquals(scalarNeuron.activation, arrayNeuron.activations[0], 0.0001, "Large negative activation")
    }
    /**
     * Test that demonstrates connections between neurons and neuron arrays.
     * This test shows how to connect:
     * 1. Neuron to Neuron (using Synapse)
     * 2. NeuronArray to NeuronArray (using WeightMatrix)
     */
    @Test
    fun `test connection types`() {
        // Create a new network for this test
        val testNet = Network()

        // Create source neurons and neuron arrays
        val sourceNeuron = Neuron()
        val sourceArray = NeuronArray(2)

        // Create target neurons and neuron arrays
        val targetNeuron = Neuron().apply {
            updateRule = SigmoidalRule()
        }
        val targetArray = NeuronArray(2).apply {
            updateRule = SigmoidalRule()
        }

        // Add all neurons and arrays to the network
        testNet.addNetworkModelsAsync(sourceNeuron, sourceArray, targetNeuron, targetArray)

        // 1. Neuron to Neuron connection (using Synapse)
        val neuronToNeuronSynapse = Synapse(sourceNeuron, targetNeuron)
        neuronToNeuronSynapse.strength = 1.0
        testNet.addNetworkModelAsync(neuronToNeuronSynapse)

        // 2. NeuronArray to NeuronArray connection (using WeightMatrix)
        val arrayToArrayWeight = WeightMatrix(sourceArray, targetArray)
        arrayToArrayWeight.setWeights(doubleArrayOf(0.5, 0.5, 0.5, 0.5))
        testNet.addNetworkModelAsync(arrayToArrayWeight)

        // Set activations for source neurons and arrays
        sourceNeuron.activation = 1.0
        sourceNeuron.clamped = true
        sourceArray.activations = doubleArrayOf(1.0, 1.0).toColumnVector()
        sourceArray.isClamped = true

        // Update the network
        testNet.update()

        // Verify that connections work as expected
        // Since we're using SigmoidalRule, the activation won't be exactly 1.0 or 0.5
        assertTrue(targetNeuron.activation > 0.9, "Neuron to Neuron connection")
        assertTrue(targetArray.activations[0] > 0.9, "Array to Array connection")
        assertTrue(targetArray.activations[1] > 0.9, "Array to Array connection")
    }
}
