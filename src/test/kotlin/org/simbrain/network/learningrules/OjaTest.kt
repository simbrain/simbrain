package org.simbrain.network.learningrules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.toMatrix

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
        net.addNetworkModelsAsync(input, output, weight, na1, na2, wm12)
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
        input.forceSetActivation(1.0)
        output.forceSetActivation(1.0)
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
        input.forceSetActivation(1.0)
        output.forceSetActivation(2.0)
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
        input.forceSetActivation(1.0)
        output.forceSetActivation(1.0)
        (weight.learningRule as OjaRule).learningRate = .1

        // Should approach 1.
        repeat(100) {
            net.update()
        }
        assertEquals(1.0, weight.strength, .01)

        // Should approach -1
        output.forceSetActivation(-1.0)
        repeat(100) {
            net.update()
        }
        assertEquals(-1.0, weight.strength, .01)

        // Should approach 1/2
        output.forceSetActivation(2.0)
        repeat(100) {
            net.update()
        }
        assertEquals(.5, weight.strength, .01)

        // Should approach 1/3
        output.forceSetActivation(3.0)
        repeat(100) {
            net.update()
        }
        assertEquals(.33, weight.strength, .01)

        // Should approach -1/3
        output.forceSetActivation(-3.0)
        repeat(100) {
            net.update()
        }
        assertEquals(-.33, weight.strength, .01)

    }

    @Test
    fun `test vectorized rule`() {
        val inputs = doubleArrayOf(1.0, -1.0).toMatrix()
        val outputs = doubleArrayOf(1.0, 2.0, -1.0).toMatrix()
        na1.activations = inputs
        na2.activations = outputs
        net.update()
        // Only uses Hebbian part
        // Expecting [[1, -1],[2,-2],[-1,1]]
        print(wm12.weightMatrix)
        // Assertions.assertArrayEquals(doubleArrayOf(1.0, 0.0, -.5), wm_v2.weightMatrix.row(0), .01)
        // Assertions.assertArrayEquals(doubleArrayOf(-1.0, 0.0, .5), wm_v2.weightMatrix.row(1), .01)
    }

}