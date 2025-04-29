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
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.toColumnVector
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

    val weightInit = Xavier()

    val commonInputs = makeMockInputs(na1.size)
    val commonTargets = makeMockTargets(na3.size)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
        }
        (na3.updateRule as BoundedUpdateRule).upperBound = 1.0
        (na3.updateRule as BoundedUpdateRule).lowerBound = -1.0
        net.addNetworkModels(na1, na2, na3, wm1, wm2)
        weightInit.initializeWeights(wm1)
        weightInit.initializeWeights(wm2)
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
            weightInit.initializeWeights(wm1)
            weightInit.initializeWeights(wm2)
            na2.randomizeBiases(NormalDistribution(0.0, .01))
            na3.randomizeBiases(NormalDistribution(0.0, .01))
            repeat(200) {
                listOf(wm1, wm2).forwardPass(inputVector)
                listOf(wm1, wm2).applyBackprop(targetVector, .01)
                // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
            }
            //println("Outputs: ${na3.activations}, SSE = ${targetVector sse na3.activations}")
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
            weightInit.initializeWeights(wm1)
            na2.randomizeBiases(NormalDistribution(0.0, 0.01))
            repeat(100) {
                listOf(wm1).forwardPass(commonInputs)
                listOf(wm1).applyBackprop(targetVector, .2)
            }
            //println("Outputs: ${na2.activations}, SSE = ${targetVector sse na2.activations}")
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
                listOf(wm1, wm2, wm3).applyBackprop(targetVector, .01)
                // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
            }
            //println("Outputs: ${na4.activations}, SSE = ${targetVector sse na4.activations}")
            assertEquals(0.0, targetVector sse na4.activations, .01)
        }
    }

    @Test
    fun `test backprop on weight matrix tree`() = runBlocking {
        with(net) {
            weightInit.initializeWeights(wm1)
            weightInit.initializeWeights(wm2)
            val na1copy = na1.copy().addToNetwork()
            val wm1copy = WeightMatrix(na1copy, na2).addToNetwork()
            na2.randomizeBiases(NormalDistribution(0.0, 0.01))
            na3.randomizeBiases(NormalDistribution(0.0, 0.01))
            val wmTree = WeightMatrixTree(listOf(na1, na1copy), na3)
            repeat(100) {
                wmTree.forwardPass(listOf(commonInputs, commonInputs))
                wmTree.applyBackprop(commonTargets, epsilon = .01)
            }
            assertEquals(0.0, commonTargets sse na3.activations, .01)
        }

    }

    @Test
    fun `test softmax with cross entropy`() {
        with(net) {
            val inputs = doubleArrayOf(1.0, 2.0, 3.0).toColumnVector()
            val targets = doubleArrayOf(0.0, 1.0, 0.0).toColumnVector()
            val inputLayer = NeuronArray(3)
            val outputLayer = NeuronArray(3).apply {
                updateRule = SoftmaxRule()
            }
            val wm = WeightMatrix(inputLayer, outputLayer)
            weightInit.initializeWeights(wm)
            addNetworkModels(inputLayer, outputLayer, wm)
            repeat(10000) {
                listOf(wm).forwardPass(inputs)
                listOf(wm).applyBackprop(targets, .01, lossFunction = BackpropLossFunction.CrossEntropy)
                // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
            }
            //println("Outputs: ${outputLayer.activations}, Cross Entropy = ${crossEntropy(outputLayer.activations, targets)}")
            assertEquals(0.0, crossEntropy(outputLayer.activations, targets), .01)
        }

    }

    @Test
    fun `train 10-7-10 auto-encoder`() {
        val inputs = Matrix.eye(10)
        val bp = BackpropNetwork(intArrayOf(10, 7, 10), null).apply {
            initWeights()
            initBiases()
            trainerConfig.learningRate = .01
            trainingSet = MatrixDataset(
                inputs = inputs,
                targets = inputs
            )
        }
        net.addNetworkModels(bp)
        val trainer = BackpropTrainer(net, bp)
        runBlocking {
            repeat(1000) {
                trainer.trainOnce()
            }
        }
        print(trainer.lastTrainingError)
        assertEquals(0.0, trainer.lastTrainingError , .15)
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