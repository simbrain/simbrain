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
 * @param trainTestSplit A value between 0 and 1 used to specify how much of the data will be used for training vs testing.
 *                            Default to 1, which means no testing set.
 */
class SupervisedModel(
    override val inputLayer: Layer,
    override val outputLayer: Layer,
    trainTestSplit: Double = 1.0,
): LocatableModel(), SupervisedNetwork {

    val layers = computeOrderedUpdatePath(inputLayer, outputLayer)

    val weightMatrices = layers.getAllOutgoingConnectors()

    val synapseGroups = layers.getAllOutgoingSynapseGroups()

    @Transient
    override val events = LocationEvents()

    override val trainerConfig: SupervisedTrainerConfig = SupervisedTrainerConfig(
        lossFunctionProvider = ::possibleLossFunctions
    ).apply {
        testConfiguration = TestConfiguration().apply { enabled = trainTestSplit < 1.0 }
    }

    override var trainingSet: MatrixDataset

    override var testingSet: MatrixDataset

    init {
        val nrows = max(inputLayer.size, outputLayer.size)

        val inputs = Matrix(nrows, inputLayer.size).applyDiagonalPattern()
        val targets = Matrix(nrows, outputLayer.size).applyDiagonalPattern()

        val (trainingData, testingData) = splitDataSet(inputs, targets, trainTestSplit)

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

    override fun trainBatch(rowRange: IntRange, probe: StructuredProbe?): Double {
        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val synapseGroupAccumulator: HashMap<SynapseGroup, Matrix> = HashMap()
        val biasesAccumulator: HashMap<Layer, Matrix> = HashMap()
        val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

        val probeContext = probe?.createMapProbe("trainBatch")

        val error = with(supervisedNetwork) {
            rowRange.sumOf { rowNum ->
                val rowProbeContext = probeContext?.createMapProbe("trainRow-$rowNum")
                inputLayer.setActivations(trainingSet.inputs.row(rowNum))
                val targetVec = trainingSet.targets.rowVectorTransposed(rowNum)
                with(network) {
                    layers.forwardPass(listOf(inputLayer.activations), inputLayers = listOf(inputLayer), rowProbeContext)
                    rowProbeContext?.write("forwardPassOutputActivations", outputLayer.activations.clone())
                    layers.accumulateBackprop(
                        targetVec,
                        outputLayer,
                        weightAccumulator,
                        synapseGroupAccumulator,
                        biasesAccumulator,
                        rawMatrixAccumulator,
                        lossFunction = config.lossFunction,
                        probe = rowProbeContext
                    )
                }
            }
        }

        val weightAccumulatorContext = probeContext?.createMapProbe("weightAccumulators")

        weightAccumulatorContext?.writeAll(weightAccumulator) { wm, delta ->
            wm.displayName to delta
        }

        weightAccumulator.forEach { (wm, delta) ->
            val weightsDelta = config.optimizer.computeDelta(wm.weights, delta)
            weightAccumulatorContext?.write("delta_${wm.displayName}") { weightsDelta.clone() }

            wm.weights.add(weightsDelta)
            weightAccumulatorContext?.write("weights_${wm.displayName}") { wm.weights.clone() }

            wm.events.updated.fire()
        }

        weightAccumulatorContext?.writeAll(synapseGroupAccumulator) { sg, delta ->
            sg.displayName to delta
        }

        synapseGroupAccumulator.forEach { (sg, delta) ->
            val weightMatrix = sg.getWeightMatrix()
            val delta = config.optimizer.computeDelta(weightMatrix, delta)
            weightAccumulatorContext?.write("delta_${sg.displayName}") { delta.clone() }

            sg.setWeightMatrix(weightMatrix.add(delta))
            weightAccumulatorContext?.write("weights_${sg.displayName}") { sg.getWeightMatrix().clone() }

            sg.events.updated.fire()
        }


        probeContext?.createMapProbe("biasesAccumulator")?.writeAll(biasesAccumulator) { na, delta ->
            na.displayName to delta
        }

        val computeDeltaContext = probeContext?.createMapProbe("computeDelta")

        biasesAccumulator.forEach { (na, delta) ->
            val delta = config.optimizer.computeDelta(na.biases, delta)
            computeDeltaContext?.write(na.displayName, delta)
            na.biases = na.biases.add(delta)
            na.events.updated.fire()
        }

        probeContext?.createMapProbe("updatedBiases")?.writeAll(biasesAccumulator) { na, _ -> na.displayName to na.biases.clone() }

        rawMatrixAccumulator.forEach { (matrix, delta) ->
            matrix.add(config.optimizer.computeDelta(matrix, delta))
        }

        probeContext?.write("rawMatrixAccumulator", rawMatrixAccumulator)

        return error / rowRange.count()
    }
}