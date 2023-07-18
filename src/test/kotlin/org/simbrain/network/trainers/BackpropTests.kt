package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.sse
import org.simbrain.util.toMatrix

class BackpropTests {

    val net = Network()
    val na1 = NeuronArray(net, 2)
    val na2 = NeuronArray(net, 3)
    val na3 = NeuronArray(net, 2)
    val wm1 = WeightMatrix(net, na1, na2)
    val wm2 = WeightMatrix(net, na2, na3)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
        }
        (na3.updateRule as BoundedUpdateRule).upperBound = 1.0
        (na3.updateRule as BoundedUpdateRule).lowerBound = -1.0
        net.addNetworkModelsAsync(na1, na2, na3, wm1, wm2)
    }

    var inputVector = doubleArrayOf(0.0, 1.0).toMatrix()
    var targetVector = doubleArrayOf(-1.0, 0.5).toMatrix()

    @Test
    fun `test backprop relu`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.Relu
        testBackprop()
    }

    @Test
    fun `test backprop sigmoid logistic`() {
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
        testBackprop()
    }

    @Test
    fun `test backprop sigmoid arctan`() {
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.ARCTAN
        }
        testBackprop()
    }

    @Test
    fun `test backprop sigmoid tanh`() {
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.TANH
        }
        testBackprop()
    }

    @Test
    fun `test backprop linear no clipping`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.NoClipping
        testBackprop()
    }

    @Test
    fun `test backprop piecewise linear`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.PiecewiseLinear
        testBackprop()
    }

    /**
     * Tests for 3 node layer case
     */
    private fun testBackprop() {
        wm1.randomize()
        wm2.randomize()
        na2.randomizeBiases()
        na3.randomizeBiases()
        repeat(100) {
            listOf(wm1, wm2).applyBackprop(inputVector, targetVector, .1)
            // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
        }
        println("Outputs: ${na3.activations}, SSE = ${targetVector sse na3.activations}")
        assertEquals(0.0, targetVector sse na3.activations, .01)
    }

    @Test
    fun `test two node layers`() {
        inputVector = doubleArrayOf(0.0, 1.0).toMatrix()
        targetVector = doubleArrayOf(1.0, 0.5, -1.0).toMatrix()
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
            lowerBound = -1.0
        }
        wm1.randomize()
        na2.randomizeBiases()
        repeat(100) {
            listOf(wm1).applyBackprop(inputVector, targetVector, .1)
        }
        println("Outputs: ${na2.activations}, SSE = ${targetVector sse na2.activations}")
        assertEquals(0.0, targetVector sse na2.activations, .01)
    }

    @Test
    fun `test four node layers`() {
        val na4 = NeuronArray(net, 2)
        val wm3 = WeightMatrix(net, na3, na4)
        net.addNetworkModelsAsync(wm3, na4)
        repeat(100) {
            listOf(wm1, wm2, wm3).applyBackprop(inputVector, targetVector, .1)
            // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
        }
        println("Outputs: ${na4.activations}, SSE = ${targetVector sse na4.activations}")
        assertEquals(0.0, targetVector sse na4.activations, .01)
    }

    @Test
    fun `test backprop on weight matrix tree`() {
        wm1.randomize()
        wm2.randomize()
        na2.randomizeBiases()
        na3.randomizeBiases()
        val wmTree = WeightMatrixTree(listOf(na1), na3)
        repeat(100) {
            wmTree.applyBackprop(listOf(inputVector), targetVector, .1)
        }
        assertEquals(0.0, targetVector sse na3.activations, .01)

    }


}