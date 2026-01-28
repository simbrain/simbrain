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

class SupervisedModelTest {

    val net = Network()
    val inputArray = NeuronArray(10)
    val outputArray = NeuronArray(10)
    val wm = WeightMatrix(inputArray, outputArray)
    val sm = SupervisedModel(inputArray, outputArray)
    init {
        net.addNetworkModelsAsync(inputArray, outputArray, wm, sm)
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

        val backpropNetwork = BackpropNetwork(intArrayOf(2,2,1), null).also { network1.addNetworkModelsAsync(it) }

        val layer1 = NeuronArray(2).also { network2.addNetworkModelsAsync(it) }.also { it.isClamped = true; it.label = "Input" }
        val layer2 = NeuronArray(2).also { network2.addNetworkModelsAsync(it) }.also { it.updateRule = SigmoidalRule(); it.label = "Hidden" }
        val layer3 = NeuronArray(1).also { network2.addNetworkModelsAsync(it) }.also { it.updateRule = SigmoidalRule(); it.label = "Output" }

        val wm1 = WeightMatrix(layer1, layer2).also { network2.addNetworkModelsAsync(it) }
        val wm2 = WeightMatrix(layer2, layer3).also { network2.addNetworkModelsAsync(it) }

        val supervisedModel = SupervisedModel(layer1, layer3).also { network2.addNetworkModelsAsync(it) }

        val SupervisedTrainer = SupervisedTrainer(network1, backpropNetwork).apply {
            config.optimizer = MomentumOptimizer()
        }
        val supervisedTrainer = SupervisedTrainer(network2, supervisedModel).apply {
            config.optimizer = MomentumOptimizer()
        }

        val trainingInputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        )

        val trainingTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        backpropNetwork.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        supervisedModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        assertEquals(SupervisedTrainer.config.learningRate, supervisedTrainer.config.learningRate) { "Learning rate should be the same" }
        assertEquals(
            SupervisedTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" },
            supervisedTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" }
        ) { "Optimizer should be the same" }

        backpropNetwork.wmList.zip(supervisedModel.weightMatrices).forEach { (bwm, swm) ->
            (swm as WeightMatrix).weights.copyFrom(bwm.weights)
        }

        with(network1) {
            backpropNetwork.inputLayer.setActivations(trainingInputs[0].toDoubleArray())
            backpropNetwork.forwardPass()
        }
        with(network2) {
            supervisedModel.inputLayer.setActivations(trainingInputs[0].toDoubleArray())
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
            with(network1) { SupervisedTrainer.trainBatch(startingIndex until startingIndex + 1, backpropProbeContext) }
            with(network2) { supervisedTrainer.trainBatch(startingIndex until startingIndex + 1, supervisedProbeContext) }



            val result = diffProbes(backpropProbe, supervisedProbe, allowMissing = true)

            assertTrue(result.isEmpty()) { result }

            assertEquals(SupervisedTrainer.lastTrainingError, supervisedTrainer.lastTrainingError) {
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

        val na1 = NeuronArray(2).also { network1.addNetworkModelsAsync(it) }.also { it.isClamped = true; it.label = "layer1" }
        val na2 = NeuronArray(2).also { network1.addNetworkModelsAsync(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer2" }
        val na3 = NeuronArray(1).also { network1.addNetworkModelsAsync(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer3" }

        val ng1 = NeuronGroup(2).also { network2.addNetworkModelsAsync(it) }.also { it.isClamped = true; it.label = "layer1" }
        val ng2 = NeuronGroup(2).also { network2.addNetworkModelsAsync(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer2" }
        val ng3 = NeuronGroup(1).also { network2.addNetworkModelsAsync(it) }.also { it.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.ARCTAN }; it.label = "layer3" }

        val nawm1 = WeightMatrix(na1, na2).also { network1.addNetworkModelsAsync(it) }.also { it.label = "wm1" }
        val nawm2 = WeightMatrix(na2, na3).also { network1.addNetworkModelsAsync(it) }.also { it.label = "wm2" }

        val ngwm1 = WeightMatrix(ng1, ng2).also { network2.addNetworkModelsAsync(it) }.also { it.label = "wm1" }
        val ngwm2 = WeightMatrix(ng2, ng3).also { network2.addNetworkModelsAsync(it) }.also { it.label = "wm2" }

        val naModel = SupervisedModel(na1, na3).also { network1.addNetworkModelsAsync(it) }

        val ngModel = SupervisedModel(ng1, ng3).also { network2.addNetworkModelsAsync(it) }

        val naTrainer = SupervisedTrainer(network1, naModel).apply {
            config.optimizer = MomentumOptimizer(0.0)
        }

        val ngTrainer = SupervisedTrainer(network2, ngModel).apply {
            config.optimizer = MomentumOptimizer(0.0)
        }

        val trainingInputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        )

        val trainingTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        naModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        ngModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        assertEquals(naTrainer.config.learningRate, ngTrainer.config.learningRate) { "Learning rate should be the same" }
        assertEquals(
            naTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" },
            ngTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" }
        ) { "Optimizer should be the same" }

        nawm1.weights.copyFrom(ngwm1.weights)
        nawm2.weights.copyFrom(ngwm2.weights)

        with(network1) {
            naModel.inputLayer.setActivations(trainingInputs[0].toDoubleArray())
            naModel.forwardPass()
        }
        with(network2) {
            ngModel.inputLayer.setActivations(trainingInputs[0].toDoubleArray())
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

        val backpropNetwork = BackpropNetwork(intArrayOf(2,2,1), null).also { network1.addNetworkModelsAsync(it) }

        backpropNetwork.layerList.forEachIndexed { index, layer -> layer.label = "layer${index + 1}" }
        backpropNetwork.wmList.forEachIndexed { index, wm -> wm.label = "wm${index + 1}" }

        val layer1 = NeuronGroup(2).also { network2.addNetworkModelsAsync(it) }.also { it.label = "layer1"; it.isClamped = true }
        val layer2 = NeuronGroup(2).also { network2.addNetworkModelsAsync(it) }.also { it.label = "layer2"; it.updateRule = SigmoidalRule() }
        val layer3 = NeuronGroup(1).also { network2.addNetworkModelsAsync(it) }.also { it.label = "layer3"; it.updateRule = SigmoidalRule() }

        val wm1 = SynapseGroup(layer1, layer2).also { it.label = "wm1"; network2.addNetworkModelsAsync(it) }
        val wm2 = SynapseGroup(layer2, layer3).also { it.label = "wm2"; network2.addNetworkModelsAsync(it) }

        val supervisedModel = SupervisedModel(layer1, layer3).also { network2.addNetworkModelsAsync(it) }

        val SupervisedTrainer = SupervisedTrainer(network1, backpropNetwork).apply {
            config.optimizer = MomentumOptimizer(0.0)
        }
        val supervisedTrainer = SupervisedTrainer(network2, supervisedModel).apply {
            config.optimizer = MomentumOptimizer(0.0)
        }

        val trainingInputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        )

        val trainingTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        backpropNetwork.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        supervisedModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        assertEquals(SupervisedTrainer.config.learningRate, supervisedTrainer.config.learningRate) { "Learning rate should be the same" }
        assertEquals(
            SupervisedTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" },
            supervisedTrainer.config.optimizer.let { "${it::class.simpleName} [${it.allPropertiesToString(", ")}]" }
        ) { "Optimizer should be the same" }

        backpropNetwork.wmList.zip(supervisedModel.weightMatrices).forEach { (bwm, swm) ->
            (swm as WeightMatrix).weights.copyFrom(bwm.weights)
        }

        backpropNetwork.wmList.zip(supervisedModel.synapseGroups).forEach { (bwm, ssg) ->
            ssg.setWeightMatrix(bwm.weights)
        }

        with(network1) {
            backpropNetwork.inputLayer.setActivations(trainingInputs[0].toDoubleArray())
            backpropNetwork.forwardPass()
        }
        with(network2) {
            supervisedModel.inputLayer.setActivations(trainingInputs[0].toDoubleArray())
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
            with(network1) { SupervisedTrainer.trainBatch(startingIndex until startingIndex + 1, backpropProbeContext) }
            with(network2) { supervisedTrainer.trainBatch(startingIndex until startingIndex + 1, supervisedProbeContext) }

            val result = diffProbes(backpropProbe, supervisedProbe, allowMissing = true)

            assertTrue(result.isEmpty()) { result }

            assertEquals(SupervisedTrainer.lastTrainingError, supervisedTrainer.lastTrainingError) {
                "Training error should be the same on training batch $startingIndex"
            }

            assertEquals(backpropNetwork.layerActivationsToString(), supervisedModel.layerActivationsToString()) {
                "Layer activations should be the same on training batch $startingIndex"
            }
        }

    }

    @Test
    fun `test automatic loss function update when output layer changes`() = runBlocking {
        val network = Network()
        val inputLayer = NeuronArray(3).also { network.addNetworkModelsAsync(it) }
        val outputLayer = NeuronArray(3).also { network.addNetworkModelsAsync(it) }
        val wm = WeightMatrix(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }
        val supervisedModel = SupervisedModel(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        // Initially, with LinearRule (default), should have SSE
        assertEquals(BackpropLossFunction.SSE, supervisedModel.trainerConfig.lossFunction)
        assertTrue(supervisedModel.trainerConfig.lossFunction.canUse(outputLayer))

        // Change to SoftmaxRule - loss function should auto-update to CrossEntropy
        outputLayer.updateRule = org.simbrain.network.updaterules.SoftmaxRule()
        
        // Give the event listener time to fire
        kotlinx.coroutines.delay(100)
        
        // Verify the loss function was automatically updated to CrossEntropy
        assertEquals(BackpropLossFunction.CrossEntropy, supervisedModel.trainerConfig.lossFunction)
        assertTrue(supervisedModel.trainerConfig.lossFunction.canUse(outputLayer))

        // Change back to SigmoidalRule - loss function should auto-update back to SSE
        outputLayer.updateRule = SigmoidalRule().apply { type = SigmoidFunctionEnum.LOGISTIC }
        
        // Give the event listener time to fire
        kotlinx.coroutines.delay(100)
        
        // Verify the loss function was automatically updated back to SSE (first in the list)
        assertEquals(BackpropLossFunction.SSE, supervisedModel.trainerConfig.lossFunction)
        assertTrue(supervisedModel.trainerConfig.lossFunction.canUse(outputLayer))
    }

    @Test
    fun `test automatic loss function update for initial softmax layer`() = runBlocking {
        val network = Network()
        val inputLayer = NeuronArray(3).also { network.addNetworkModelsAsync(it) }
        
        // Create output layer with SoftmaxRule BEFORE creating the SupervisedModel
        val outputLayer = NeuronArray(3).apply {
            updateRule = org.simbrain.network.updaterules.SoftmaxRule()
        }.also { network.addNetworkModelsAsync(it) }
        
        val wm = WeightMatrix(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }
        
        // Create SupervisedModel - it should detect the SoftmaxRule and set CrossEntropy immediately
        val supervisedModel = SupervisedModel(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        // Verify the loss function was automatically set to CrossEntropy at initialization
        assertEquals(BackpropLossFunction.CrossEntropy, supervisedModel.trainerConfig.lossFunction)
        assertTrue(supervisedModel.trainerConfig.lossFunction.canUse(outputLayer))
    }

    @Test
    fun `network update should match trainer forward pass before training with NeuronArray`() = runBlocking {
        val network = Network()

        val inputLayer = NeuronArray(2).also { network.addNetworkModelsAsync(it) }.also { 
            it.isClamped = true
            it.label = "Input"
        }
        val hiddenLayer = NeuronArray(2).also { network.addNetworkModelsAsync(it) }.also { 
            it.updateRule = SigmoidalRule()
            it.label = "Hidden"
        }
        val outputLayer = NeuronArray(1).also { network.addNetworkModelsAsync(it) }.also { 
            it.updateRule = SigmoidalRule()
            it.label = "Output"
        }

        val wm1 = WeightMatrix(inputLayer, hiddenLayer).also { network.addNetworkModelsAsync(it) }
        val wm2 = WeightMatrix(hiddenLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val supervisedModel = SupervisedModel(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val testInput = mutableListOf(1.0, 0.0)

        with(network) {
            inputLayer.setActivations(testInput.toDoubleArray())
            
            println("=== FORWARD PASS ===")
            println("Input layer activations: ${inputLayer.activationArray.contentToString()}")
            println("Hidden layer before: ${hiddenLayer.activationArray.contentToString()}")
            println("Output layer before: ${outputLayer.activationArray.contentToString()}")
            supervisedModel.forwardPass()
            println("Hidden layer after forwardPass: ${hiddenLayer.activationArray.contentToString()}")
            println("Output layer after forwardPass: ${outputLayer.activationArray.contentToString()}")
            val forwardPassOutput = outputLayer.activationArray.clone()
            
            inputLayer.setActivations(testInput.toDoubleArray())
            hiddenLayer.clear()
            outputLayer.clear()
            
            println("\n=== NETWORK UPDATE ===")
            println("Input layer activations: ${inputLayer.activationArray.contentToString()}")
            println("Hidden layer before: ${hiddenLayer.activationArray.contentToString()}")
            println("Output layer before: ${outputLayer.activationArray.contentToString()}")
            network.bufferedUpdate()
            println("Hidden layer after update: ${hiddenLayer.activationArray.contentToString()}")
            println("Output layer after update: ${outputLayer.activationArray.contentToString()}")
            val networkUpdateOutput = outputLayer.activationArray.clone()
            
            assertArrayEquals(forwardPassOutput, networkUpdateOutput, 1e-10) {
                "BEFORE training: network.update() output ${networkUpdateOutput.contentToString()} should match forwardPass() output ${forwardPassOutput.contentToString()}"
            }
        }
    }

    @Test
    fun `network update should match trainer forward pass after training with NeuronArray`() = runBlocking {
        val network = Network()

        val inputLayer = NeuronArray(2).also { network.addNetworkModelsAsync(it) }.also { 
            it.isClamped = true
            it.label = "Input"
        }
        val hiddenLayer = NeuronArray(2).also { network.addNetworkModelsAsync(it) }.also { 
            it.updateRule = SigmoidalRule()
            it.label = "Hidden"
        }
        val outputLayer = NeuronArray(1).also { network.addNetworkModelsAsync(it) }.also { 
            it.updateRule = SigmoidalRule()
            it.label = "Output"
        }

        val wm1 = WeightMatrix(inputLayer, hiddenLayer).also { network.addNetworkModelsAsync(it) }
        val wm2 = WeightMatrix(hiddenLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val supervisedModel = SupervisedModel(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val trainingInputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        )

        val trainingTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        supervisedModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        val trainer = SupervisedTrainer(network, supervisedModel)
        
        with(network) {
            repeat(100) {
                trainer.trainBatch(0 until trainingInputs.size)
            }
        }

        trainingInputs.forEachIndexed { index, input ->
            with(network) {
                inputLayer.setActivations(input.toDoubleArray())
                
                supervisedModel.forwardPass()
                val forwardPassOutput = outputLayer.activationArray.clone()
                
                inputLayer.setActivations(input.toDoubleArray())
                network.bufferedUpdate()
                val networkUpdateOutput = outputLayer.activationArray.clone()
                
                assertArrayEquals(forwardPassOutput, networkUpdateOutput, 1e-10) {
                    "For input $index ($input): network.update() output ${networkUpdateOutput.contentToString()} should match forwardPass() output ${forwardPassOutput.contentToString()}"
                }
            }
        }
    }

    @Test
    fun `network update should match trainer forward pass after training with NeuronGroup`() = runBlocking {
        val network = Network()

        val inputLayer = NeuronGroup(2).also { network.addNetworkModelsAsync(it) }.also { 
            it.isClamped = true
            it.label = "Input"
        }
        val hiddenLayer = NeuronGroup(2).also { network.addNetworkModelsAsync(it) }.also { 
            it.updateRule = SigmoidalRule()
            it.label = "Hidden"
        }
        val outputLayer = NeuronGroup(1).also { network.addNetworkModelsAsync(it) }.also { 
            it.updateRule = SigmoidalRule()
            it.label = "Output"
        }

        val sg1 = SynapseGroup(inputLayer, hiddenLayer).also { network.addNetworkModelsAsync(it) }
        val sg2 = SynapseGroup(hiddenLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val supervisedModel = SupervisedModel(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val trainingInputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        )

        val trainingTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        supervisedModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        val trainer = SupervisedTrainer(network, supervisedModel)
        
        with(network) {
            repeat(100) {
                trainer.trainBatch(0 until trainingInputs.size)
            }
        }

        trainingInputs.forEachIndexed { index, input ->
            with(network) {
                inputLayer.setActivations(input.toDoubleArray())
                
                supervisedModel.forwardPass()
                val forwardPassOutput = outputLayer.activationArray.clone()
                
                inputLayer.setActivations(input.toDoubleArray())
                network.bufferedUpdate()
                val networkUpdateOutput = outputLayer.activationArray.clone()
                
                assertArrayEquals(forwardPassOutput, networkUpdateOutput, 1e-10) {
                    "For input $index ($input): network.update() output ${networkUpdateOutput.contentToString()} should match forwardPass() output ${forwardPassOutput.contentToString()}"
                }
            }
        }
    }

    @Test
    fun `network update should match trainer forward pass after training with NeuronCollection`() = runBlocking {
        val network = Network()

        val inputNeurons = (0 until 2).map { Neuron().also { network.addNetworkModelsAsync(it) } }
        val hiddenNeurons = (0 until 2).map { Neuron().also { network.addNetworkModelsAsync(it) } }
        val outputNeurons = (0 until 1).map { Neuron().also { network.addNetworkModelsAsync(it) } }

        val inputLayer = NeuronCollection(inputNeurons).also { network.addNetworkModelsAsync(it) }.also { 
            it.isClamped = true
            it.label = "Input"
        }
        val hiddenLayer = NeuronCollection(hiddenNeurons).also { network.addNetworkModelsAsync(it) }.also { 
            it.setNeuronType(SigmoidalRule())
            it.label = "Hidden"
        }
        val outputLayer = NeuronCollection(outputNeurons).also { network.addNetworkModelsAsync(it) }.also { 
            it.setNeuronType(SigmoidalRule())
            it.label = "Output"
        }

        val sg1 = SynapseGroup(inputLayer, hiddenLayer).also { network.addNetworkModelsAsync(it) }
        val sg2 = SynapseGroup(hiddenLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val supervisedModel = SupervisedModel(inputLayer, outputLayer).also { network.addNetworkModelsAsync(it) }

        val trainingInputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        )

        val trainingTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        supervisedModel.trainingSet = TrainingDataset(
            inputs = trainingInputs,
            targets = trainingTargets
        )

        val trainer = SupervisedTrainer(network, supervisedModel)
        
        with(network) {
            repeat(100) {
                trainer.trainBatch(0 until trainingInputs.size)
            }
        }

        trainingInputs.forEachIndexed { index, input ->
            with(network) {
                inputLayer.setActivations(input.toDoubleArray())
                
                supervisedModel.forwardPass()
                val forwardPassOutput = outputLayer.activationArray.clone()
                
                inputLayer.setActivations(input.toDoubleArray())
                network.bufferedUpdate()
                val networkUpdateOutput = outputLayer.activationArray.clone()
                
                assertArrayEquals(forwardPassOutput, networkUpdateOutput, 1e-10) {
                    "For input $index ($input): network.update() output ${networkUpdateOutput.contentToString()} should match forwardPass() output ${forwardPassOutput.contentToString()}"
                }
            }
        }
    }

}