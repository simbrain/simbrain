package org.simbrain.network.subnetworks

import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnTrainerConfig
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.copy
import org.simbrain.util.copyFrom
import org.simbrain.util.stats.ProbabilityDistribution

/**
 * A subnetwork wrapper around an existing CNN pipeline:
 * Tensor -> [Conv/Pool -> Tensor]* -> Flatten -> NeuronArray -> [WeightMatrix -> NeuronArray]*.
 *
 * Unlike a thin wrapper model, this subnetwork provides staged update semantics so one
 * [Network.update] iteration performs a full forward sweep through the CNN pipeline.
 */
class ConvolutionalNeuralNetwork(
    val inputTensorLayer: TensorLayer,
    val outputArray: NeuronArray,
) : Subnetwork() {

    val trainerConfig = CnnTrainerConfig()

    /** Ordered tensor connectors (conv/pool) discovered by walking from [inputTensorLayer]. */
    private val tensorConnectors: List<TensorConnector>

    /** Ordered tensor stages (output of each conv/pool connector). */
    private val tensorLayerStages: List<TensorLayer>

    /** The flatten connector bridging the last tensor to the first NeuronArray. */
    private val flattenConnector: FlattenConnector

    /** Ordered dense-layer weight matrices from flatten target to [outputArray]. */
    private val denseWeightMatrices: List<WeightMatrix>

    /** Ordered dense-layer NeuronArrays (targets of each weight matrix). */
    private val denseNeuronArrays: List<NeuronArray>

    /** All NeuronArrays in order: flatten target, then dense layers. */
    private val allNeuronArrays: List<NeuronArray>

    var trainingSet: TrainingDataset = TrainingDataset(
        inputs = mutableListOf(),
        targets = mutableListOf(),
        inputSize = inputTensorLayer.shape.size,
        targetSize = outputArray.size
    )

    var testingSet: TrainingDataset = TrainingDataset(
        inputs = mutableListOf(),
        targets = mutableListOf(),
        inputSize = inputTensorLayer.shape.size,
        targetSize = outputArray.size
    )

    init {
        // Discover pipeline by walking the graph from inputTensor
        val connectors = mutableListOf<TensorConnector>()
        val stages = mutableListOf<TensorLayer>()
        var currentTensor = inputTensorLayer

        while (true) {
            val flattenOut = currentTensor.outgoingFlattenConnectors
            if (flattenOut.isNotEmpty()) {
                flattenConnector = flattenOut.first()
                break
            }
            val outgoing = currentTensor.outgoingTensorConnectors
            check(outgoing.isNotEmpty()) {
                "Pipeline broken: Tensor '${currentTensor.displayName}' has no outgoing connectors"
            }
            val connector = outgoing.first()
            connectors.add(connector)
            stages.add(connector.target)
            currentTensor = connector.target
        }
        tensorConnectors = connectors
        tensorLayerStages = stages

        // Walk WeightMatrix chain from flatten target to outputArray
        val wms = mutableListOf<WeightMatrix>()
        val nas = mutableListOf<NeuronArray>()
        var currentLayer: Layer = flattenConnector.target
        while (currentLayer != outputArray) {
            val wm = currentLayer.outgoingConnectors.filterIsInstance<WeightMatrix>().first()
            wms.add(wm)
            nas.add(wm.target as NeuronArray)
            currentLayer = wm.target as Layer
        }
        denseWeightMatrices = wms
        denseNeuronArrays = nas
        allNeuronArrays = buildList {
            add(flattenConnector.target as NeuronArray)
            addAll(nas)
        }

        // Register all pipeline elements as children of this subnetwork.
        val pipelineComponents = buildList {
            add(inputTensorLayer)
            addAll(tensorConnectors)
            addAll(tensorLayerStages)
            add(flattenConnector)
            add(flattenConnector.target)
            addAll(denseNeuronArrays)
            addAll(denseWeightMatrices)
        }.distinct()

        addModels(pipelineComponents)

        // If any pipeline component is removed, remove this wrapper as well.
        pipelineComponents.forEach { component ->
            component.events.deleted.on {
                delete()
            }
        }
    }

    context(Network)
    override fun update() {
        // Forward through CNN tensor stages (TensorConnector.propagate + Tensor.update)
        for (i in tensorConnectors.indices) {
            tensorLayerStages[i].inputs.fill(0.0)
            tensorConnectors[i].propagate()
            tensorLayerStages[i].update()
        }

        // Flatten + dense layers: use standard Layer accumulate/update cycle.
        // NeuronArray.accumulateInputs() handles both WeightMatrix PSR and FlattenConnector propagation.
        for (na in allNeuronArrays) {
            na.accumulateInputs()
            na.update()
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        tensorConnectors.filterIsInstance<ConvolutionConnector>().forEach {
            it.heInitialize()
            it.clearGrads()
        }
        tensorLayerStages.forEach {
            it.biases.fill(0.0)
            it.clearGradients()
        }
        denseWeightMatrices.forEach {
            config.weightInitializationStrategy.initializeWeights(it)
        }
        denseNeuronArrays.forEach {
            it.biasArray.fill(0.0)
        }
    }

    override fun copy(): ConvolutionalNeuralNetwork {
        // Deep-copy the entire pipeline: create new components with same structure.

        // Copy input tensor
        val newInput = TensorLayer(inputTensorLayer.shape).apply {
            System.arraycopy(inputTensorLayer.activations, 0, activations, 0, activations.size)
            System.arraycopy(inputTensorLayer.biases, 0, biases, 0, biases.size)
            isClamped = inputTensorLayer.isClamped
            label = inputTensorLayer.label
            location = inputTensorLayer.location
        }

        // Copy tensor stages and connectors
        var prevTensor = newInput
        for (i in tensorConnectors.indices) {
            val origTensor = tensorLayerStages[i]
            val newTensorLayer = TensorLayer(origTensor.shape).apply {
                System.arraycopy(origTensor.biases, 0, biases, 0, biases.size)
                label = origTensor.label
                location = origTensor.location
            }

            when (val origConn = tensorConnectors[i]) {
                is ConvolutionConnector -> {
                    ConvolutionConnector(prevTensor, newTensorLayer, origConn.kernelSize, origConn.numFilters, origConn.stride, origConn.padding).apply {
                        System.arraycopy(origConn.kernels, 0, kernels, 0, kernels.size)
                        System.arraycopy(origConn.filterBiases, 0, filterBiases, 0, filterBiases.size)
                    }
                }
                is PoolingConnector -> {
                    PoolingConnector(prevTensor, newTensorLayer, origConn.poolSize, origConn.stride, origConn.poolingType)
                }
            }
            prevTensor = newTensorLayer
        }

        // Copy flatten and dense chain
        val origFlatTarget = flattenConnector.target as NeuronArray
        val newFlatArray = NeuronArray(origFlatTarget.size).apply {
            label = origFlatTarget.label
            location = origFlatTarget.location
        }
        FlattenConnector(prevTensor, newFlatArray)

        var prevLayer: Layer = newFlatArray
        var newOutput: NeuronArray = newFlatArray
        for (i in denseWeightMatrices.indices) {
            val origNa = denseNeuronArrays[i]
            val newNa = NeuronArray(origNa.size).apply {
                updateRule = origNa.updateRule.copy()
                biases.copyFrom(origNa.biases)
                label = origNa.label
                location = origNa.location
            }
            WeightMatrix(prevLayer, newNa).apply {
                copyFrom(denseWeightMatrices[i])
            }
            prevLayer = newNa
            newOutput = newNa
        }

        return ConvolutionalNeuralNetwork(newInput, newOutput).apply {
            label = this@ConvolutionalNeuralNetwork.label
            trainingSet = this@ConvolutionalNeuralNetwork.trainingSet.copy()
            testingSet = this@ConvolutionalNeuralNetwork.testingSet.copy()
            trainerConfig.learningRate = this@ConvolutionalNeuralNetwork.trainerConfig.learningRate
            trainerConfig.beta1 = this@ConvolutionalNeuralNetwork.trainerConfig.beta1
            trainerConfig.beta2 = this@ConvolutionalNeuralNetwork.trainerConfig.beta2
            trainerConfig.batchSize = this@ConvolutionalNeuralNetwork.trainerConfig.batchSize
            trainerConfig.lossFunction = this@ConvolutionalNeuralNetwork.trainerConfig.lossFunction
            trainerConfig.computeAccuracy = this@ConvolutionalNeuralNetwork.trainerConfig.computeAccuracy
            trainerConfig.weightInitializationStrategy = this@ConvolutionalNeuralNetwork.trainerConfig.weightInitializationStrategy.copy()
            trainerConfig.testConfiguration = this@ConvolutionalNeuralNetwork.trainerConfig.testConfiguration.copy()
            trainerConfig.stoppingCondition = this@ConvolutionalNeuralNetwork.trainerConfig.stoppingCondition.copy()
        }
    }

    private val config get() = trainerConfig
}
