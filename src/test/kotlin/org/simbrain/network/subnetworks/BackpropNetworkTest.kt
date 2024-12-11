package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedTrainer.UpdateMethod
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.math.SigmoidFunctionEnum
import smile.math.matrix.Matrix

class BackpropNetworkTest {

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(10,8,10), null).apply {
        label = "backprop"
        trainingSet = MatrixDataset(
            inputs = Matrix.eye(10),
            targets = Matrix.eye(10)
        )
        outputLayer.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
    }

    init {
        net.addNetworkModels(bp)
        bp.initBiases()
        bp.initWeights()
    }

    @Test
    fun `test backprop learning`() {
        bp.trainer.updateType = UpdateMethod.Epoch()
        bp.trainer.learningRate = 0.04
        with(net) {
            with(bp) {
                runBlocking {
                    repeat(1000) {
                        trainer.trainOnce()
                    }
                }
            }
        }

        assert(bp.trainer.lastTrainingError < 0.1) { "Error: ${bp.trainer.lastTrainingError}" }

    }

    @Test
    fun `test softmax backprop learning`() {
        bp.outputLayer.updateRule = SoftmaxRule()
        bp.trainer.lossFunction = org.simbrain.network.trainers.BackpropLossFunction.CrossEntropy
        bp.trainer.learningRate = 0.1
        with(net) {
            with(bp) {
                runBlocking {
                    repeat(1000) {
                        trainer.trainOnce()
                    }
                }
            }
        }

        println("Bacprop error ${bp.trainer.lastTrainingError}")
        assert(bp.trainer.lastTrainingError < 0.1) { "Error: ${bp.trainer.lastTrainingError}" }

    }

    @Test
    fun `test backprop network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(BackpropNetwork::class.java, "backprop"))
    }

}