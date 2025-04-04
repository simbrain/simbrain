package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.allPropertiesToString
import org.simbrain.util.copyFrom
import smile.math.matrix.Matrix

class SupervisedModelTest {

    val net = Network()
    val inputArray = NeuronArray(10)
    val outputArray = NeuronArray(10)
    val wm = WeightMatrix(inputArray, outputArray)
    val sm = SupervisedModel(inputArray, outputArray)
    init {
        net.addNetworkModels(inputArray, outputArray, wm, sm)
    }

    @Test
    fun `deleting supervised model does not delete constituents`() = runBlocking {
        sm.delete()
        assertTrue(!net.allModels.contains(sm))
        assertTrue(net.allModels.contains(inputArray))
        assertTrue(net.allModels.contains(outputArray))
    }

    @Test
    fun `test supervised model serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        assertNotNull(fromXml.getModels(SupervisedModel::class.java).first())
    }

    @Test
    fun `simple supervised models should perform identically as backprop networks`() = runBlocking {

        val network1 = Network()

        val network2 = Network()

        val backpropNetwork = BackpropNetwork(intArrayOf(2,2,1), null).also { network1.addNetworkModels(it) }

        val layer1 = NeuronArray(2).also { network2.addNetworkModels(it) }.also { it.isClamped = true }
        val layer2 = NeuronArray(2).also { network2.addNetworkModels(it) }.also { it.updateRule = SigmoidalRule() }
        val layer3 = NeuronArray(1).also { network2.addNetworkModels(it) }.also { it.updateRule = SigmoidalRule() }

        val wm1 = WeightMatrix(layer1, layer2).also { network2.addNetworkModels(it) }
        val wm2 = WeightMatrix(layer2, layer3).also { network2.addNetworkModels(it) }

        val supervisedModel = SupervisedModel(layer1, layer3).also { network2.addNetworkModels(it) }

        val backpropTrainer = BackpropTrainer(network1, backpropNetwork).apply {
            config.optimizer = MomentumOptimizer()
        }
        val supervisedTrainer = SupervisedModelTrainer(network2, supervisedModel).apply {
            config.optimizer = MomentumOptimizer()
        }

        val trainingInputs = Matrix.of(arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 1.0)
        ))

        val trainingTargets = Matrix.of(arrayOf(
            doubleArrayOf(0.0),
            doubleArrayOf(1.0),
            doubleArrayOf(1.0),
            doubleArrayOf(0.0)
        ))

        backpropNetwork.trainingSet = MatrixDataset(
            inputs = trainingInputs.clone(),
            targets = trainingTargets.clone()
        )

        supervisedModel.trainingSet = MatrixDataset(
            inputs = trainingInputs.clone(),
            targets = trainingTargets.clone()
        )

        assertEquals(backpropTrainer.config.learningRate, supervisedTrainer.config.learningRate) { "Learning rate should be the same" }
        assertEquals(
            backpropTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" },
            supervisedTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" }
        ) { "Optimizer should be the same" }

        backpropNetwork.wmList.zip(supervisedModel.weightMatrices).forEach { (bwm, swm) ->
            (swm as WeightMatrix).weights.copyFrom(bwm.weights)
        }

        with(network1) {
            backpropNetwork.inputLayer.setActivations(trainingInputs.row(0))
            backpropNetwork.forwardPass()
        }
        with(network2) {
            supervisedModel.inputLayer.setActivations(trainingInputs.row(0))
            supervisedModel.forwardPass()
        }

        fun BackpropNetwork.layerActivationsToString(): String = layerList.mapIndexed { i, na ->
            "$i: ${na.activationArray.joinToString(", ")}"
        }.joinToString("\n")

        fun SupervisedModel.layerActivationsToString(): String = layers.mapIndexed { i, layer ->
            "$i: ${layer.activationArray.joinToString(", ")}"
        }.joinToString("\n")

        assertEquals(backpropNetwork.layerActivationsToString(), supervisedModel.layerActivationsToString()) {
            "Layer activations should be the same"
        }

        (0 until 4).forEach { startingIndex ->
            val backpropProbeDataCollector = LinkedHashMap<String, Any>()
            val supervisedProbeDataCollector = LinkedHashMap<String, Any>()
            fun dataCollector(dataCollector: LinkedHashMap<String, Any>): (data: Any) -> Unit {
                return { data: Any ->
                    val (key, value) = (data as Pair<*, *>)
                    dataCollector[key as String] = value!!
                }
            }
            with(network1) { backpropTrainer.trainBatch(startingIndex until startingIndex + 1, dataCollector(backpropProbeDataCollector)) }
            with(network2) { supervisedTrainer.trainBatch(startingIndex until startingIndex + 1, dataCollector(supervisedProbeDataCollector)) }

            backpropProbeDataCollector.keys.union(supervisedProbeDataCollector.keys).forEach { key ->
                assertEquals(backpropProbeDataCollector[key], supervisedProbeDataCollector[key]) {
                    "Data $key should be the same on training batch $startingIndex"
                }
            }

            assertEquals(backpropTrainer.lastTrainingError, supervisedTrainer.lastTrainingError) {
                "Training error should be the same on training batch $startingIndex"
            }

            assertEquals(backpropNetwork.layerActivationsToString(), supervisedModel.layerActivationsToString()) {
                "Layer activations should be the same on training batch $startingIndex"
            }
        }

    }

}