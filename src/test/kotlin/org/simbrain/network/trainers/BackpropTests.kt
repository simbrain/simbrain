package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.randomizeBiases
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
    val na1 = NeuronArray(10).apply { isClamped = true }
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
    private fun testBackprop(
        inputVector: Matrix,
        targetVector: Matrix,
        supervisedModel: SupervisedModel = SupervisedModel(na1, na3),
        nRuns: Int = 200,
    ) = runBlocking {
        with(net) {
            weightInit.initializeWeights(wm1)
            weightInit.initializeWeights(wm2)
            na2.randomizeBiases(NormalDistribution(0.0, .01))
            na3.randomizeBiases(NormalDistribution(0.0, .01))
            supervisedModel.trainingSet = MatrixDataset(inputVector.transpose(), targetVector.transpose())
            val trainer = SupervisedTrainer(net, supervisedModel)
            repeat(nRuns) {
                trainer.trainOnce()
            }
            assertEquals(0.0, targetVector sse supervisedModel.outputLayer.activations, .01)
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
            val supervisedModel = SupervisedModel(na1, na2)
            testBackprop(commonInputs, targetVector, supervisedModel, nRuns = 100)
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
            val supervisedModel = SupervisedModel(na1, na4)
            testBackprop(commonInputs, targetVector, supervisedModel, nRuns = 100)
            //println("Outputs: ${na4.activations}, SSE = ${targetVector sse na4.activations}")
            assertEquals(0.0, targetVector sse na4.activations, .01)
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
            val supervisedModel = SupervisedModel(inputLayer, outputLayer)
            supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
            testBackprop(inputs, targets, supervisedModel, nRuns = 10000)
            //println("Outputs: ${outputLayer.activations}, Cross Entropy = ${crossEntropy(outputLayer.activations, targets)}")
            assertEquals(0.0, crossEntropy(outputLayer.activations, targets), .01)
        }

    }

    @Test
    fun `train 10-7-10 auto-encoder`() {
        val inputs = Matrix.eye(10)
        val bp = BackpropNetwork(intArrayOf(10, 5, 10), null).apply {
            initWeights()
            initBiases()
            trainerConfig.learningRate = .01
            trainingSet = MatrixDataset(
                inputs = inputs,
                targets = inputs
            )
        }
        net.addNetworkModels(bp)
        val trainer = SupervisedTrainer(net, bp)
        runBlocking {
            repeat(1000) {
                trainer.trainOnce()
            }
        }
        print(trainer.lastTrainingError)
        assertEquals(0.0, trainer.lastTrainingError , .01)
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