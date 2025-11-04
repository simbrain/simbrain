package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.trainers.SupervisedTrainer.TestConfiguration
import org.simbrain.util.*
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import java.awt.geom.Point2D

/**
 * A type of [SupervisedNetwork] that can be assembled from existing components that are already in the network.
 * Can be transiently assembled on the fly to represent a supervised learning task.
 *
 * @param trainTestSplit A value between 0 and 1 used to specify how much of the data will be used for training vs testing.
 *                            Default to 1, which means no testing set.
 */
class SupervisedModel(
    override val inputLayer: Layer,
    override val outputLayer: Layer,
    trainTestSplit: Double = 1.0,
): LocatableModel(), SupervisedNetwork {

    override val layers = computeOrderedUpdatePath(setOf(inputLayer), outputLayer)

    val weightMatrices = layers.getAllOutgoingConnectors()

    val synapseGroups = layers.getAllOutgoingSynapseGroups()

    @Transient
    override val events = LocationEvents()

    override val trainerConfig: SupervisedTrainerConfig = SupervisedTrainerConfig(
        lossFunctionProvider = ::possibleLossFunctions
    ).apply {
        testConfiguration = TestConfiguration().apply { enabled = trainTestSplit < 1.0 }
    }

    override var trainingSet: TrainingDataset

    override var testingSet: TrainingDataset

    init {

        val initialData = createSimpleBinaryDataset(inputLayer.size, outputLayer.size)
        val (trainingData, testingData) = splitDataSet(initialData, 0.8)
        val trainingInputs = trainingData.inputs
        val trainingTargets = trainingData.targets
        val testingInputs = testingData.inputs
        val testingTargets = testingData.targets

        trainingSet = if (inputLayer is ActivationSequence) {
            val inputSize = inputLayer.size * inputLayer.sequenceSize
            val targetSize = if (outputLayer is ActivationSequence) {
                outputLayer.size * outputLayer.sequenceSize
            } else {
                outputLayer.size
            }
            TrainingDataset(
                // If the layer is an activation sequence, data are currently flattened
                inputs = zeroMutableList(10, inputSize),
                targets = zeroMutableList(10, targetSize),
                inputSize = inputSize,
                targetSize = targetSize
            )
        } else {
            TrainingDataset(
                inputs = trainingInputs.copy(),
                targets = trainingTargets.copy(),
                inputSize = inputLayer.size,
                targetSize = outputLayer.size
            )
        }

        testingSet = if (inputLayer is ActivationSequence) {
            val inputSize = inputLayer.size * inputLayer.sequenceSize
            val targetSize = if (outputLayer is ActivationSequence) {
                outputLayer.size * outputLayer.sequenceSize
            } else {
                outputLayer.size
            }
            TrainingDataset(
                // If the layer is an activation sequence, data are currently flattened
                inputs = zeroMutableList(10, inputSize),
                targets = zeroMutableList(10, targetSize),
                inputSize = inputSize,
                targetSize = targetSize
            )
        } else {
            TrainingDataset(
                inputs = testingInputs,
                targets = testingTargets,
                inputSize = inputLayer.size,
                targetSize = outputLayer.size
            )
        }
        
        (layers + weightMatrices).forEach {
            it.events.deleted.on {
                delete()
            }
        }
        
        // Listen for output layer update rule changes and auto-adjust loss function if needed
        setupLossFunctionAutoUpdate()
    }

    /**
     * Register component relationships needed for proper copy/paste with undo/redo
     * This is called when the model is added to a network
     */
    context(Network)
    override fun shouldAdd(): Boolean {
        // Register layers as children of this model
        childToParentMap[inputLayer] = this@SupervisedModel
        childToParentMap[outputLayer] = this@SupervisedModel
        
        // Register all weight matrices as children of this model
        weightMatrices.forEach { matrix ->
            childToParentMap[matrix] = this@SupervisedModel
        }
        
        return true
    }

    override var location: Point2D
        get() = layers.centerLocation
        set(newLocation) {
            val delta = newLocation - location
            layers.forEach { it.location += delta }
        }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        initWeights()
        initBiases()
    }

    override fun initWeights() {
        weightMatrices.forEach { trainerConfig.weightInitializationStrategy.initializeWeights(it as WeightMatrix) }
        synapseGroups.forEach {
            val weightMatrix = it.getWeightMatrix()
            trainerConfig.weightInitializationStrategy.initializeWeights(weightMatrix)
            it.setWeightMatrix(weightMatrix)
        }
        layers.filterIsInstance<TransformerBlock>().forEach { it.initWeights(trainerConfig.weightInitializationStrategy) }
    }

    override fun initBiases() {
        layers.forEach {
            it.clear()
            (it as? NeuronArray)?.randomizeBiases()
            (it as? AbstractNeuronCollection)?.randomizeBiases()
            (it as? TransformerBlock)?.initBiases()
        }
    }

    context(Network)
    override fun forwardPass() {
        layers.forwardPass(listOf(inputLayer.activations), inputLayers = listOf(inputLayer))
    }

    context(Network)
    suspend fun applyImmediateLearning() {
        val isInputClamped = inputLayer.isClamped
        val output = outputLayer.activations.toDoubleArray()

        inputLayer.isClamped = true
        trainingSet = TrainingDataset(
            inputs = inputLayer.activations.transpose().clone().toMutableListOfLists(),
            targets = Matrix.row(output).toMutableListOfLists(),
            inputSize = inputLayer.size,
            targetSize = outputLayer.size
        )
        SupervisedTrainer(this@Network, this@SupervisedModel).trainOnce()

        inputLayer.isClamped = isInputClamped
        outputLayer.setActivations(output)
        events.updated.fire()
    }

    override suspend fun delete(): List<NetworkModel> {
        events.deleted.fire(this).await()
        return listOf(this)
    }

    override suspend fun afterRestore(context: Any?) {
        super.afterRestore(context)
        layers.forEach { it.afterRestore(context) }
        weightMatrices.forEach { it.afterRestore(context) }
        synapseGroups.forEach { it.afterRestore(context) }
    }

    override fun toString(): String {
        return """
                Name: $displayName
                Input: ${inputLayer.displayName} (${inputLayer.shapeString}) 
                Output: ${outputLayer.displayName} (${outputLayer.shapeString}) 
            """.trimIndent()
    }
}
