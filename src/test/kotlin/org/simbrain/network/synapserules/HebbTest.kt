package org.simbrain.network.synapserules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.synapse_update_rules.HebbianRule
import org.simbrain.util.toMatrix

class HebbTest {

    var net = Network()

    // Scalar based tests
    val n1 = Neuron(net)
    val n2 = Neuron(net)
    var s12 = Synapse(n1,n2)

    // Array based tests
    val na1 = NeuronArray(net, 2)
    val na2 = NeuronArray(net, 2)
    var wm12 = WeightMatrix(net, na1, na2)

    init {
        net.addNetworkModelsAsync(n1, n2, s12, na1, na2, wm12)
        s12.learningRule = HebbianRule().apply {
            learningRate = 1.0
        }
        s12.strength = 0.0
        n1.isClamped = true
        n2.isClamped = true
        na1.isClamped = true
        na2.isClamped = true
        wm12.hardClear()
        wm12.prototypeRule = HebbianRule().apply {
            learningRate = 1.0
        }
    }

    @Test
    fun `test basic update`() {
        n1.forceSetActivation(1.0)
        n2.forceSetActivation(1.0)
        net.update()
        assertEquals(1.0,s12.strength )
        net.update()
        assertEquals(2.0,s12.strength )
        println("Strength is ${s12.strength}")
    }

    @Test
    fun `test different learning rates`() {
        n1.forceSetActivation(1.0)
        n2.forceSetActivation(1.0)
        (s12.learningRule as HebbianRule).learningRate = .25
        net.update()
        assertEquals(.25,s12.strength )
        net.update()
        assertEquals(.5,s12.strength )
    }

    @Test
    fun `test other activations`() {
        n1.forceSetActivation(-1.0)
        n2.forceSetActivation(1.0)
        net.update()
        assertEquals(-1.0,s12.strength )
    }

    @Test
    fun `test hitting the max`() {
        n1.forceSetActivation(10.0)
        n2.forceSetActivation(10.0)
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
        // Expecting [[2,1],[6,3]]
        assertArrayEquals(doubleArrayOf(2.0,1.0), wm12.weightMatrix.row(0))
        assertArrayEquals(doubleArrayOf(6.0,3.0), wm12.weightMatrix.row(1))
        net.update()
        // Expecting [[4,2],[12,6]]
        assertArrayEquals(doubleArrayOf(4.0,2.0), wm12.weightMatrix.row(0))
        assertArrayEquals(doubleArrayOf(12.0,6.0), wm12.weightMatrix.row(1))
    }

    @Test
    fun `test vectorized rule with small learning rate`() {
        val inputs = doubleArrayOf(1.0,3.0).toMatrix()
        val outputs = doubleArrayOf(2.0,1.0).toMatrix()
        na1.activations = inputs
        na2.activations = outputs
        (wm12.prototypeRule as HebbianRule).learningRate = .1
        net.update()
        // Expecting [[.2,.1],[.6,.3]]
        assertArrayEquals(doubleArrayOf(.2,.1), wm12.weightMatrix.row(0), .01)
        assertArrayEquals(doubleArrayOf(.6,.3), wm12.weightMatrix.row(1), .01)
    }

    @Test
    fun `test vectorized rule for 2-to-3 case`() {
        val inputs = doubleArrayOf(1.0,3.0).toMatrix()
        val outputs = doubleArrayOf(2.0,1.0).toMatrix()
        na1.activations = inputs
        na2.activations = outputs
        (wm12.prototypeRule as HebbianRule).learningRate = .1
        net.update()
        // Expecting [[.2,.1],[.6,.3]]
        assertArrayEquals(doubleArrayOf(.2,.1), wm12.weightMatrix.row(0), .01)
        assertArrayEquals(doubleArrayOf(.6,.3), wm12.weightMatrix.row(1), .01)
    }

}