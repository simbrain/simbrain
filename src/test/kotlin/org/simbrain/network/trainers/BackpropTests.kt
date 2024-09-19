package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.util.crossEntropy
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.sse
import org.simbrain.util.toMatrix
import smile.math.matrix.Matrix
import kotlin.random.Random

class BackpropTests {

    val net = Network()
    val na1 = NeuronArray(10)
    val na2 = NeuronArray(7)
    val na3 = NeuronArray(10).apply {
        updateRule = LinearRule().apply {
            clippingType = LinearRule.ClippingType.NoClipping
        }
    }
    val wm1 = WeightMatrix(na1, na2)
    val wm2 = WeightMatrix(na2, na3)

    val commonInputs = makeMockInputs(na1.size)
    val commonTargets = makeMockTargets(na3.size)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
        }
        (na3.updateRule as BoundedUpdateRule).upperBound = 1.0
        (na3.updateRule as BoundedUpdateRule).lowerBound = -1.0
        net.addNetworkModels(na1, na2, na3, wm1, wm2)
    }

    @Test
    fun `test backprop relu`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.Relu
        testBackprop(commonInputs, commonTargets)
    }

    @Test
    fun `test backprop sigmoid logistic`() {
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
        testBackprop(commonInputs, commonTargets)
    }

    @Test
    fun `test backprop sigmoid arctan`() {
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.ARCTAN
        }
        testBackprop(commonInputs, commonTargets)
    }

    @Test
    fun `test backprop sigmoid tanh`() {
        na2.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.TANH
        }
        testBackprop(commonInputs, commonTargets)
    }

    @Test
    fun `test backprop linear no clipping`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.NoClipping
        testBackprop(commonInputs, commonTargets)
    }

    @Test
    fun `test backprop piecewise linear`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.PiecewiseLinear
        testBackprop(commonInputs, commonTargets)
    }

    /**
     * Tests for 3 node layer case
     */
    private fun testBackprop(inputVector: Matrix, targetVector: Matrix) {
        with(net) {
            wm1.randomize()
            wm2.randomize()
            na2.randomizeBiases()
            na3.randomizeBiases()
            repeat(100) {
                listOf(wm1, wm2).forwardPass(inputVector)
                listOf(wm1, wm2).applyBackprop(targetVector, .1)
                // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
            }
            println("Outputs: ${na3.activations}, SSE = ${targetVector sse na3.activations}")
            assertEquals(0.0, targetVector sse na3.activations, .01)
        }
    }

    @Test
    fun `test two node layers`() {
        with(net) {
            val targetVector = makeMockTargets(na2.size)
            na2.updateRule = SigmoidalRule().apply {
                type = SigmoidFunctionEnum.LOGISTIC
                lowerBound = 0.0
            }
            wm1.randomize()
            na2.randomizeBiases()
            repeat(100) {
                listOf(wm1).forwardPass(commonInputs)
                listOf(wm1).applyBackprop(targetVector, .2)
            }
            println("Outputs: ${na2.activations}, SSE = ${targetVector sse na2.activations}")
            assertEquals(0.0, targetVector sse na2.activations, .01)
        }
    }

    @Test
    fun `test four node layers`() {
        with(net) {
            val na4 = NeuronArray(10)
            val wm3 = WeightMatrix(na3, na4)

            val targetVector = makeMockTargets(na4.size)

            net.addNetworkModels(wm3, na4)
            repeat(100) {
                listOf(wm1, wm2, wm3).forwardPass(commonInputs)
                listOf(wm1, wm2, wm3).applyBackprop(targetVector, .1)
                // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
            }
            println("Outputs: ${na4.activations}, SSE = ${targetVector sse na4.activations}")
            assertEquals(0.0, targetVector sse na4.activations, .01)
        }
    }

    @Test
    fun `test backprop on weight matrix tree`() = runBlocking {
        with(net) {
            wm1.randomize()
            wm2.randomize()
            val na1copy = na1.copy().addToNetwork()
            val wm1copy = WeightMatrix(na1copy, na2).addToNetwork()
            na2.randomizeBiases()
            na3.randomizeBiases()
            val wmTree = WeightMatrixTree(listOf(na1, na1copy), na3)
            repeat(100) {
                wmTree.forwardPass(listOf(commonInputs, commonInputs))
                wmTree.applyBackprop(commonTargets, .1)
            }
            assertEquals(0.0, commonTargets sse na3.activations, .01)
        }

    }

    @Test
    fun `test softmax with cross entropy`() {
        with(net) {
            val inputs = doubleArrayOf(1.0, 2.0, 3.0).toMatrix()
            val targets = doubleArrayOf(0.0, 1.0, 0.0).toMatrix()
            val inputLayer = NeuronArray(3)
            val outputLayer = NeuronArray(3).apply {
                updateRule = SoftmaxRule()
            }
            val wm = WeightMatrix(inputLayer, outputLayer)
            wm.randomize()
            addNetworkModels(inputLayer, outputLayer, wm)
            repeat(10000) {
                listOf(wm).forwardPass(inputs)
                listOf(wm).applyBackprop(targets, .1, lossFunction = ::crossEntropy)
                // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
            }
            println("Outputs: ${outputLayer.activations}, Cross Entropy = ${crossEntropy(outputLayer.activations, targets)}")
            assertEquals(0.0, crossEntropy(outputLayer.activations, targets), .01)
        }

    }

    @Test
    fun `train 10-7-10 auto-encoder`() {
        val inputs = Matrix.eye(10)
        val bp = BackpropNetwork(intArrayOf(10, 7, 10), null).apply {
            outputLayer.updateRule = SigmoidalRule().apply {
                type = SigmoidFunctionEnum.LOGISTIC
            }
            trainer.learningRate = .1
            trainingSet = MatrixDataset(
                inputs = inputs,
                targets = inputs
            )
        }
        net.addNetworkModels(bp)
        with(net) {
            with(bp) {
                runBlocking {
                    repeat(1000) {
                        bp.trainer.trainOnce()
                    }
                }
            }
        }
        // Does not get that low after 1K
        assertEquals(0.0, bp.trainer.lastError, .1)
    }

    fun makeMockInputs(size: Int): Matrix {
        val inputs = Matrix(size, 1)
        for (i in 0 until size) {
            inputs[i, 0] = Random.nextDouble(0.0, 1.0)
        }
        return inputs
    }

    fun makeMockTargets(size: Int): Matrix {
        val targets = Matrix(size, 1)
        for (i in 0 until size) {
            targets[i, 0] = if (i % 2 == 0) 1.0 else 0.0
        }
        return targets
    }

}