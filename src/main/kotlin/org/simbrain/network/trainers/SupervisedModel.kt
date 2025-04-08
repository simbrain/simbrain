package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.trainers.SupervisedTrainer.TestConfiguration
import org.simbrain.util.*
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import java.awt.geom.Point2D
import kotlin.math.max

/**
 * @param trainTestSplitRatio A value between 0 and 1 used to specify how much of the data will be used for training vs testing.
 *                            Set to 1 if for no testing set.
 */
class SupervisedModel(
    override val inputLayer: Layer,
    override val outputLayer: Layer,
    trainTestSplitRatio: Double = 0.8,
): LocatableModel(), SupervisedNetwork {

    val layers = computeOrderedUpdatePath(inputLayer, outputLayer)

    val weightMatrices = layers.getAllOutgoingConnectors()

    val synapseGroups = layers.getAllOutgoingSynapseGroups()

    @Transient
    override val events = LocationEvents()

    override val trainerConfig: SupervisedTrainerConfig = SupervisedTrainerConfig(
        lossFunctionProvider = ::possibleLossFunctions
    ).apply {
        testConfiguration = TestConfiguration().apply { enabled = trainTestSplitRatio < 1.0 }
    }

    override var trainingSet: MatrixDataset

    override var testingSet: MatrixDataset

    init {
        val nrows = max(inputLayer.size, outputLayer.size)

        val inputs = Matrix(nrows, inputLayer.size).applyDiagonalPattern()
        val targets = Matrix(nrows, outputLayer.size).applyDiagonalPattern()

        val (trainingData, testingData) = splitDataSet(inputs, targets, trainTestSplitRatio)

        val (trainingInputs, trainingTargets) = trainingData
        val (testingInputs, testingTargets) = testingData

        trainingSet = if (inputLayer is ActivationSequence) {
            MatrixDataset(
                // If the layer is an activation sequence, data are currently flattened
                inputs = Matrix(10, inputLayer.size * inputLayer.sequenceSize),
                targets = Matrix(10, outputLayer.size)
            )
        } else {
            MatrixDataset(
                inputs = trainingInputs,
                targets = trainingTargets
            )
        }

        testingSet = if (inputLayer is ActivationSequence) {
            MatrixDataset(
                // If the layer is an activation sequence, data are currently flattened
                inputs = Matrix(10, inputLayer.size * inputLayer.sequenceSize),
                targets = Matrix(10, outputLayer.size)
            )
        } else {
            MatrixDataset(
                inputs = testingInputs,
                targets = testingTargets
            )
        }
    }

    override var location: Point2D
        get() = layers.centerLocation
        set(newLocation) {
            val delta = newLocation - location
            layers.forEach { it.location += delta }
        }

    init {
        listOf(inputLayer, outputLayer).forEach {
            it.events.deleted.on {
                delete()
            }
        }
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
            (it as? TransformerBlock)?.initBiases()
        }
    }

    context(Network)
    override suspend fun forwardPass() {
        layers.forwardPass(listOf(inputLayer.activations), inputLayers = listOf(inputLayer))
    }

    context(Network)
    suspend fun applyImmediateLearning() {
        val isInputClamped = inputLayer.isClamped
        val output = outputLayer.activations.toDoubleArray()

        inputLayer.isClamped = true
        trainingSet = MatrixDataset(
            inputLayer.activations.transpose().clone(),
            Matrix.row(output),
        )
        SupervisedModelTrainer(this@Network, this@SupervisedModel).trainOnce()

        inputLayer.isClamped = isInputClamped
        outputLayer.setActivations(output)
        events.updated.fire()
    }

    override suspend fun delete(): List<NetworkModel> {
        events.deleted.fire(this).await()
        return listOf(this)
    }
}

class SupervisedModelTrainer(network: Network, supervisedModel: SupervisedModel): SupervisedTrainer<SupervisedModel>(network, supervisedModel) {

    override fun trainRow(rowNum: Int): Double {
        return trainBatch(rowNum until rowNum + 1)
    }

    override fun trainBatch(rowRange: IntRange, probe: (Any) -> Unit): Double {
        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val synapseGroupAccumulator: HashMap<SynapseGroup, Matrix> = HashMap()
        val biasesAccumulator: HashMap<Layer, Matrix> = HashMap()
        val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

        val error = with(supervisedNetwork) {
            rowRange.sumOf { rowNum ->
                inputLayer.setActivations(trainingSet.inputs.row(rowNum))
                val targetVec = trainingSet.targets.rowVectorTransposed(rowNum)
                with(network) {
                    layers.forwardPass(listOf(inputLayer.activations), inputLayers = listOf(inputLayer))
                    layers.accumulateBackprop(
                        targetVec,
                        outputLayer,
                        weightAccumulator,
                        synapseGroupAccumulator,
                        biasesAccumulator,
                        rawMatrixAccumulator,
                        lossFunction = config.lossFunction
                    )
                }
            }
        }

        weightAccumulator.forEach { (wm, delta) ->
            wm.weights.add(config.optimizer.computeDelta(wm.weights, delta))
            wm.events.updated.fire()
        }

        probe("weightAccumulator" to weightAccumulator)

        synapseGroupAccumulator.forEach { (sg, delta) ->
            val weightMatrix = sg.getWeightMatrix()
            val delta = config.optimizer.computeDelta(weightMatrix, delta)
            sg.setWeightMatrix(weightMatrix.add(delta))
            sg.events.updated.fire()
        }

        probe("synapseGroupAccumulator" to synapseGroupAccumulator)

        biasesAccumulator.forEach { (na, delta) ->
            na.biases = na.biases.add(config.optimizer.computeDelta(na.biases, delta))
            na.events.updated.fire()
        }

        probe("biasesAccumulator" to biasesAccumulator)

        rawMatrixAccumulator.forEach { (matrix, delta) ->
            matrix.add(config.optimizer.computeDelta(matrix, delta))
        }

        probe("rawMatrixAccumulator" to rawMatrixAccumulator)

        return error / rowRange.count()
    }
}