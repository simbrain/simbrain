package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.AdamOptimizer
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.SupervisedTrainer.UpdateMethod
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.identityMutableList
import org.simbrain.util.math.SigmoidFunctionEnum

class BackpropNetworkTest {

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(10,8,10), null).apply {
        label = "backprop"
        trainingSet = TrainingDataset(
            inputs = identityMutableList(10),
            targets = identityMutableList(10)
        )
        outputLayer.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
    }

    init {
        net.addNetworkModelsAsync(bp)
        bp.initBiases()
        bp.initWeights()
    }

    @Test
    fun `test backprop learning`() {
        bp.trainerConfig.updateType = UpdateMethod.Epoch()
        bp.trainerConfig.learningRate = 0.01
        bp.trainerConfig.optimizer = AdamOptimizer()

        val trainer = SupervisedTrainer(net, bp)

        runBlocking {
            repeat(2000) {
                trainer.trainOnce()
            }
        }

        assert(trainer.lastTrainingError < 0.1) { "Error: ${trainer.lastTrainingError}" }

    }

    @Test
    fun `test softmax backprop learning`() {
        bp.outputLayer.updateRule = SoftmaxRule()
        bp.trainerConfig.lossFunction = org.simbrain.network.trainers.BackpropLossFunction.CrossEntropy
        bp.trainerConfig.learningRate = 0.1

        val trainer = SupervisedTrainer(net, bp)

        runBlocking {
            repeat(1000) {
                trainer.trainOnce()
            }
        }

        println("Bacprop error ${trainer.lastTrainingError}")
        assert(trainer.lastTrainingError < 0.1) { "Error: ${trainer.lastTrainingError}" }

    }

    @Test
    fun `test backprop network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(BackpropNetwork::class.java, "backprop"))
    }

    @Test
    fun `test automatic loss function update when output layer changes`() = runBlocking {
        val network = Network()
        val backpropNet = BackpropNetwork(intArrayOf(3, 3), null)
        network.addNetworkModelsAsync(backpropNet)

        // Initially, with SigmoidalRule (default for output layer), should have SSE
        Assertions.assertEquals(org.simbrain.network.trainers.BackpropLossFunction.SSE, backpropNet.trainerConfig.lossFunction)
        Assertions.assertTrue(backpropNet.trainerConfig.lossFunction.canUse(backpropNet.outputLayer))

        // Change to SoftmaxRule - loss function should auto-update to CrossEntropy
        backpropNet.outputLayer.updateRule = SoftmaxRule()
        
        // Give the event listener time to fire
        kotlinx.coroutines.delay(100)
        
        // Verify the loss function was automatically updated to CrossEntropy
        Assertions.assertEquals(org.simbrain.network.trainers.BackpropLossFunction.CrossEntropy, backpropNet.trainerConfig.lossFunction)
        Assertions.assertTrue(backpropNet.trainerConfig.lossFunction.canUse(backpropNet.outputLayer))

        // Change back to SigmoidalRule - loss function should auto-update back to SSE
        backpropNet.outputLayer.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.LOGISTIC }
        
        // Give the event listener time to fire
        kotlinx.coroutines.delay(100)
        
        // Verify the loss function was automatically updated back to SSE (first in the list)
        Assertions.assertEquals(org.simbrain.network.trainers.BackpropLossFunction.SSE, backpropNet.trainerConfig.lossFunction)
        Assertions.assertTrue(backpropNet.trainerConfig.lossFunction.canUse(backpropNet.outputLayer))
    }

}