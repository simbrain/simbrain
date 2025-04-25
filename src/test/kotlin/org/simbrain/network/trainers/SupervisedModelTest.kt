package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.allPropertiesToString
import org.simbrain.util.copyFrom
import org.simbrain.util.format
import org.simbrain.util.math.SigmoidFunctionEnum
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

        val backpropProbe = StructuredProbe.ListProbe()
        val supervisedProbe = StructuredProbe.ListProbe()

        (0 until 4).forEach { startingIndex ->
            val backpropProbeContext = backpropProbe.createMapProbe()
            val supervisedProbeContext = supervisedProbe.createMapProbe()
            with(network1) { backpropTrainer.trainBatch(startingIndex until startingIndex + 1, backpropProbeContext) }
            with(network2) { supervisedTrainer.trainBatch(startingIndex until startingIndex + 1, supervisedProbeContext) }



            val result = diffProbes(backpropProbe, supervisedProbe, allowMissing = true)

            assertTrue(result.isEmpty()) { result }

            assertEquals(backpropTrainer.lastTrainingError, supervisedTrainer.lastTrainingError) {
                "Training error should be the same on training batch $startingIndex"
            }

            assertEquals(backpropNetwork.layerActivationsToString(), supervisedModel.layerActivationsToString()) {
                "Layer activations should be the same on training batch $startingIndex"
            }
        }

    }

    @Test
    fun `neuron group supervised models should perform identically as neuron array supervised models`() = runBlocking {

        val network1 = Network()

        val network2 = Network()

        val na1 = NeuronArray(2).also { network1.addNetworkModels(it) }.also { it.isClamped = true; it.label = "layer1" }
        val na2 = NeuronArray(2).also { network1.addNetworkModels(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer2" }
        val na3 = NeuronArray(1).also { network1.addNetworkModels(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer3" }

        val ng1 = NeuronGroup(2).also { network2.addNetworkModels(it) }.also { it.isClamped = true; it.label = "layer1" }
        val ng2 = NeuronGroup(2).also { network2.addNetworkModels(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer2" }
        val ng3 = NeuronGroup(1).also { network2.addNetworkModels(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer3" }

        val nawm1 = WeightMatrix(na1, na2).also { network1.addNetworkModels(it) }.also { it.label = "wm1" }
        val nawm2 = WeightMatrix(na2, na3).also { network1.addNetworkModels(it) }.also { it.label = "wm2" }

        val ngwm1 = WeightMatrix(ng1, ng2).also { network2.addNetworkModels(it) }.also { it.label = "wm1" }
        val ngwm2 = WeightMatrix(ng2, ng3).also { network2.addNetworkModels(it) }.also { it.label = "wm2" }

        val naModel = SupervisedModel(na1, na3).also { network1.addNetworkModels(it) }

        val ngModel = SupervisedModel(ng1, ng3).also { network2.addNetworkModels(it) }

        val naTrainer = SupervisedModelTrainer(network1, naModel).apply {
            config.optimizer = MomentumOptimizer(0.0)
        }

        val ngTrainer = SupervisedModelTrainer(network2, ngModel).apply {
            config.optimizer = MomentumOptimizer(0.0)
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

        naModel.trainingSet = MatrixDataset(
            inputs = trainingInputs.clone(),
            targets = trainingTargets.clone()
        )

        ngModel.trainingSet = MatrixDataset(
            inputs = trainingInputs.clone(),
            targets = trainingTargets.clone()
        )

        assertEquals(naTrainer.config.learningRate, ngTrainer.config.learningRate) { "Learning rate should be the same" }
        assertEquals(
            naTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" },
            ngTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" }
        ) { "Optimizer should be the same" }

        nawm1.weights.copyFrom(ngwm1.weights)
        nawm2.weights.copyFrom(ngwm2.weights)

        with(network1) {
            naModel.inputLayer.setActivations(trainingInputs.row(0))
            naModel.forwardPass()
        }
        with(network2) {
            ngModel.inputLayer.setActivations(trainingInputs.row(0))
            ngModel.forwardPass()
        }

        fun SupervisedModel.layerActivationsToString(digits: Int = 6): String = layers.mapIndexed { i, layer ->
            "$i: ${layer.activationArray.joinToString(", ") { it.format(digits)}}"
        }.joinToString("\n")

        assertEquals(naModel.layerActivationsToString(), ngModel.layerActivationsToString()) {
            "Layer activations should be the same"
        }

        val naProbe = StructuredProbe.ListProbe()
        val ngProbe = StructuredProbe.ListProbe()

        (0 until 4).forEach { startingIndex ->
            val naProbeContext = naProbe.createMapProbe()
            val ngProbeContext = ngProbe.createMapProbe()
            with(network1) { naTrainer.trainBatch(startingIndex until startingIndex + 1, naProbeContext) }
            with(network2) { ngTrainer.trainBatch(startingIndex until startingIndex + 1, ngProbeContext) }

            val result = diffProbes(naProbe, ngProbe)

            assertTrue(result.isEmpty()) {
                println("=====================")
                println(naProbe.toTreeString())
                println("---------------------")
                println(ngProbe.toTreeString())
                println("=====================")
                result
            }

            assertEquals(naTrainer.lastTrainingError, ngTrainer.lastTrainingError) {
                "Training error should be the same on training batch $startingIndex"
            }

            assertEquals(naModel.layerActivationsToString(), ngModel.layerActivationsToString()) {
                "Layer activations should be the same on training batch $startingIndex"
            }
        }

    }


    @Test
    fun `simple supervised models with synapse group should perform identically as backprop networks`() = runBlocking {

        val network1 = Network()

        val network2 = Network()

        val backpropNetwork = BackpropNetwork(intArrayOf(2,2,1), null).also { network1.addNetworkModels(it) }

        backpropNetwork.layerList.forEachIndexed { index, layer -> layer.label = "layer${index + 1}" }
        backpropNetwork.wmList.forEachIndexed { index, wm -> wm.label = "wm${index + 1}" }

        val layer1 = NeuronGroup(2).also { network2.addNetworkModels(it) }.also { it.label = "layer1"; it.isClamped = true }
        val layer2 = NeuronGroup(2).also { network2.addNetworkModels(it) }.also { it.label = "layer2"; it.updateRule = SigmoidalRule() }
        val layer3 = NeuronGroup(1).also { network2.addNetworkModels(it) }.also { it.label = "layer3"; it.updateRule = SigmoidalRule() }

        val wm1 = SynapseGroup(layer1, layer2).also { it.label = "wm1"; network2.addNetworkModels(it) }
        val wm2 = SynapseGroup(layer2, layer3).also { it.label = "wm2"; network2.addNetworkModels(it) }

        val supervisedModel = SupervisedModel(layer1, layer3).also { network2.addNetworkModels(it) }

        val backpropTrainer = BackpropTrainer(network1, backpropNetwork).apply {
            config.optimizer = MomentumOptimizer(0.0)
        }
        val supervisedTrainer = SupervisedModelTrainer(network2, supervisedModel).apply {
            config.optimizer = MomentumOptimizer(0.0)
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

        backpropNetwork.wmList.zip(supervisedModel.synapseGroups).forEach { (bwm, ssg) ->
            ssg.setWeightMatrix(bwm.weights)
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
            "$i: ${na.activationArray.joinToString(", ") { it.format(6) }}"
        }.joinToString("\n")

        fun SupervisedModel.layerActivationsToString(): String = layers.mapIndexed { i, layer ->
            "$i: ${layer.activationArray.joinToString(", ") { it.format(6) }}"
        }.joinToString("\n")

        assertEquals(backpropNetwork.layerActivationsToString(), supervisedModel.layerActivationsToString()) {
            "Layer activations should be the same"
        }

        val backpropProbe = StructuredProbe.ListProbe()
        val supervisedProbe = StructuredProbe.ListProbe()

        (0 until 4).forEach { startingIndex ->
            val backpropProbeContext = backpropProbe.createMapProbe()
            val supervisedProbeContext = supervisedProbe.createMapProbe()
            with(network1) { backpropTrainer.trainBatch(startingIndex until startingIndex + 1, backpropProbeContext) }
            with(network2) { supervisedTrainer.trainBatch(startingIndex until startingIndex + 1, supervisedProbeContext) }

            val result = diffProbes(backpropProbe, supervisedProbe, allowMissing = true)

            assertTrue(result.isEmpty()) { result }

            assertEquals(backpropTrainer.lastTrainingError, supervisedTrainer.lastTrainingError) {
                "Training error should be the same on training batch $startingIndex"
            }

            assertEquals(backpropNetwork.layerActivationsToString(), supervisedModel.layerActivationsToString()) {
                "Layer activations should be the same on training batch $startingIndex"
            }
        }

    }

}