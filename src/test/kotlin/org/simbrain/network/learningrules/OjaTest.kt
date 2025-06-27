package org.simbrain.network.learningrules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.toColumnVector

class OjaTest {

    // 2->1 network
    var net = Network()
    val input = Neuron()
    val output = Neuron()
    var weight = Synapse(input,output)

    // For array based tests
    val na1 = NeuronArray(2)
    val na2 = NeuronArray(3)
    var wm12 = WeightMatrix(na1, na2)

    init {
        net.addNetworkModels(input, output, weight, na1, na2, wm12)
        weight.learningRule = OjaRule().apply {
            learningRate = 1.0
            normalizationFactor = 1.0
        }
        weight.strength = 0.0
        weight.upperBound = 10.0
        weight.lowerBound = -10.0
        input.clamped = true
        output.clamped = true

        na1.isClamped = true
        na2.isClamped = true
        wm12.hardClear()
        wm12.learningRule = OjaRule().apply {
            learningRate = 1.0
            normalizationFactor = 1.0
        }
    }

    @Test
    fun `test update for a single weight and clamped nodes`() {
        input.activation = 1.0
        output.activation = 1.0
        net.update()

        // delta-w  = rate (out(in - out * weight))
        //          = 1 (1(1 - 1*0)) = 1
        assertEquals(1.0, weight.strength)
        net.update()
        //          1 (1(1 - 1*1)) = 0
        assertEquals(1.0, weight.strength)
        repeat(10) {
            net.update()
        }
        assertEquals(1.0, weight.strength)
    }

    @Test
    fun `test update with different source and target`() {
        // High learning rate leads to divergence in this case
        input.activation = 1.0
        output.activation = 2.0
        // delta-w  = rate (out(in - out * weight))
        //          = 1 (2(1 - 2*0)) = 2
        // Weight becomes 0 + 2 = 2
        net.update()
        assertEquals(2.0, weight.strength)
        //          = 1 (2(1 - 2*2)) = -6
        // Weight becomes 2 -6 = -4
        net.update()
        assertEquals(-4.0, weight.strength)
        //          = 1 (2(1 - 2*-4)) = 18
        // Weight becomes -6 + 18 = 12, which is clipped at 10
        net.update()
        assertEquals(10.0, weight.strength)
    }

    @Test
    fun `test update with learning rate less than 1`() {
        input.activation = 1.0
        output.activation = 1.0
        (weight.learningRule as OjaRule).learningRate = .1

        // Should approach 1.
        repeat(100) {
            net.update()
        }
        assertEquals(1.0, weight.strength, .01)

        // Should approach -1
        output.activation = -1.0
        repeat(100) {
            net.update()
        }
        assertEquals(-1.0, weight.strength, .01)

        // Should approach 1/2
        output.activation = 2.0
        repeat(100) {
            net.update()
        }
        assertEquals(.5, weight.strength, .01)

        // Should approach 1/3
        output.activation = 3.0
        repeat(100) {
            net.update()
        }
        assertEquals(.33, weight.strength, .01)

        // Should approach -1/3
        output.activation = -3.0
        repeat(100) {
            net.update()
        }
        assertEquals(-.33, weight.strength, .01)

    }

    @Test
    fun `test vectorized rule`() {
        val inputs = doubleArrayOf(1.0, -1.0).toColumnVector()
        val outputs = doubleArrayOf(1.0, 2.0, -1.0).toColumnVector()
        na1.activations = inputs
        na2.activations = outputs
        net.update()
        // Only uses Hebbian part
        assertArrayEquals(doubleArrayOf(1.0, -1.0), wm12.weights.row(0))
        assertArrayEquals(doubleArrayOf(2.0, -2.0), wm12.weights.row(1))
        assertArrayEquals(doubleArrayOf(-1.0, 1.0), wm12.weights.row(2))
    }

    @Test
    fun `weight norm converges to sqrt of normalization factor`() {
        val inputSize = 5
        val outputNeuron = Neuron().apply { clamped = true }
        val inputs = (1..inputSize).map { Neuron().apply { clamped = true } }
        val synapses = inputs.map { input ->
            Synapse(input, outputNeuron).apply {
                learningRule = OjaRule().apply {
                    learningRate = 0.1
                    normalizationFactor = 4.0
                }
                strength = Math.random() - 0.5
                upperBound = 10.0
                lowerBound = -10.0
            }
        }

        val net = Network()
        net.addNetworkModels(outputNeuron)
        net.addNetworkModels(inputs)
        net.addNetworkModels(synapses)

        repeat(1000) {
            inputs.forEach { it.activation = Math.random() - 0.5 }
            val y = synapses.sumOf { it.source.activation * it.strength }
            outputNeuron.activation = y
            net.update()
        }

        val norm = Math.sqrt(synapses.sumOf { it.strength * it.strength })
        assertEquals(2.0, norm, 0.1)  // sqrt(4.0) = 2.0
    }

}