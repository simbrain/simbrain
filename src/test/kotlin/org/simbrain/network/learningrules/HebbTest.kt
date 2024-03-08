package org.simbrain.network.learningrules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.toMatrix

class HebbTest {

    var net = Network()

    // For scalar based tests
    val n1 = Neuron()
    val n2 = Neuron()
    var s12 = Synapse(n1,n2)

    // For array based tests
    val na1 = NeuronArray(2)
    val na2 = NeuronArray(2)
    var wm12 = WeightMatrix(na1, na2)

    init {
        net.addNetworkModelsAsync(n1, n2, s12, na1, na2, wm12)
        s12.learningRule = HebbianRule().apply {
            learningRate = 1.0
        }
        s12.strength = 0.0
        n1.clamped = true
        n2.clamped = true
        na1.isClamped = true
        na2.isClamped = true
        wm12.hardClear()
        wm12.learningRule = HebbianRule().apply {
            learningRate = 1.0
        }
    }

    @Test
    fun `test basic update`() {
        n1.activation = 1.0
        n2.activation = 1.0
        net.update()
        assertEquals(1.0,s12.strength )
        net.update()
        assertEquals(2.0,s12.strength )
        println("Strength is ${s12.strength}")
    }

    @Test
    fun `test different learning rates`() {
        n1.activation = 1.0
        n2.activation = 1.0
        (s12.learningRule as HebbianRule).learningRate = .25
        net.update()
        assertEquals(.25,s12.strength )
        net.update()
        assertEquals(.5,s12.strength )
    }

    @Test
    fun `test other activations`() {
        n1.activation = -1.0
        n2.activation = 1.0
        net.update()
        assertEquals(-1.0,s12.strength )
    }

    @Test
    fun `test hitting the max`() {
        n1.activation = 10.0
        n2.activation = 10.0
        repeat(100) {
            net.update()
        }
        assertEquals(s12.upperBound, s12.strength )
    }

    @Test
    fun `test vectorized rule`() {
        val inputs = doubleArrayOf(1.0,3.0).toMatrix()
        val outputs = doubleArrayOf(2.0,1.0).toMatrix()
        na1.activations = inputs
        na2.activations = outputs
        net.update()
        // Weights start at 0
        // Expecting [[2,6],[1,3]]
        assertArrayEquals(doubleArrayOf(2.0,6.0), wm12.weightMatrix.row(0))
        assertArrayEquals(doubleArrayOf(1.0,3.0), wm12.weightMatrix.row(1))
        net.update()
        // Expecting [[4,12],[2,6]]
        assertArrayEquals(doubleArrayOf(4.0,12.0), wm12.weightMatrix.row(0))
        assertArrayEquals(doubleArrayOf(2.0,6.0), wm12.weightMatrix.row(1))
    }

    @Test
    fun `test vectorized rule with small learning rate`() {
        val inputs = doubleArrayOf(1.0,3.0).toMatrix()
        val outputs = doubleArrayOf(2.0,1.0).toMatrix()
        na1.activations = inputs
        na2.activations = outputs
        (wm12.learningRule as HebbianRule).learningRate = .1
        net.update()
        // Expecting [[.2,.6],[.1,.3]]
        assertArrayEquals(doubleArrayOf(.2,.6), wm12.weightMatrix.row(0), .01)
        assertArrayEquals(doubleArrayOf(.1,.3), wm12.weightMatrix.row(1), .01)
    }

    @Test
    fun `test vectorized rule for 3-to-2 case`() {
        val inputs = doubleArrayOf(2.0, 0.0, -1.0).toMatrix()
        val outputs = doubleArrayOf(.5, -.5).toMatrix()
        val na1_v2 = NeuronArray(3)
        val wm_v2 = WeightMatrix(na1_v2, na2).apply {
            learningRule = HebbianRule().apply {
                learningRate = 1.0
            }
        }
        wm_v2.hardClear()
        net.addNetworkModelsAsync(na1_v2, wm_v2)
        na1_v2.isClamped = true
        na1_v2.activations = inputs
        na2.activations = outputs
        net.update()
        // Expecting [[1.0, 0.0, -.5],[-1.0, 0.0, .5]]
        print(wm_v2.weightMatrix)
        assertArrayEquals(doubleArrayOf(1.0, 0.0, -.5), wm_v2.weightMatrix.row(0), .01)
        assertArrayEquals(doubleArrayOf(-1.0, 0.0, .5), wm_v2.weightMatrix.row(1), .01)
    }

}

