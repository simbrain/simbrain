package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.events.LocationEvents
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.rowVectorTransposed
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import java.awt.geom.Point2D

class SupervisedModel(
    override val inputLayer: ArrayLayer,
    override val outputLayer: ArrayLayer,
    private val useImmediateLearning: Boolean = true
): LocatableModel(), SupervisedNetwork {

    val layers = computeUpdateOrderList(outputLayer)

    val weightMatrices = layers.flatMap { it.outgoingConnectors }

    @Transient
    override val events = LocationEvents()

    override val trainerConfig: SupervisedTrainerConfig = SupervisedTrainerConfig(lossFunctionProvider = ::possibleLossFunctions)

    override var trainingSet: MatrixDataset = if(useImmediateLearning) { MatrixDataset(
        inputLayer.activations.transpose().clone(),
        outputLayer.activations.transpose().clone()
    )} else {
        // TODO: Temp so it runs
        MatrixDataset(
            inputs = Matrix(10,inputLayer.size * (inputLayer as ActivationSequence).sequenceSize),
            targets = Matrix(10, outputLayer.size)
        )
    }

    override var testingSet: MatrixDataset = MatrixDataset(
        inputs = Matrix(10,inputLayer.size * (inputLayer as ActivationSequence).sequenceSize),
        targets = Matrix(10, outputLayer.size)
    )

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
    override fun forwardPass() {
        layers.forwardPass(listOf(inputLayer.activations), inputLayers = listOf(inputLayer))
    }

    override suspend fun delete(): List<NetworkModel> {
        events.deleted.fire(this).await()
        return listOf(this)
    }
}

class SupervisedModelTrainer(network: Network, supervisedModel: SupervisedModel): SupervisedTrainer<SupervisedModel>(network, supervisedModel) {

    override fun trainRow(rowNum: Int): Double {
        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val biasesAccumulator: HashMap<ArrayLayer, Matrix> = HashMap()
        val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

        val error = with(supervisedNetwork) {
            inputLayer.setActivations(trainingSet.inputs.row(rowNum))
            val targetVec = trainingSet.targets.rowVectorTransposed(rowNum)
            with(network) {
                layers.forwardPass(listOf(inputLayer.activations), inputLayers = listOf(inputLayer))
                layers.accumulateBackprop(targetVec, outputLayer, weightAccumulator, biasesAccumulator, rawMatrixAccumulator, lossFunction = config.lossFunction)
            }
        }

        weightAccumulator.forEach { (wm, delta) ->
            wm.weights.add(config.optimizer.computeDelta(wm.weights, delta))
            wm.events.updated.fire()
        }

        biasesAccumulator.forEach { (na, delta) ->
            na.biases.add(config.optimizer.computeDelta(na.biases, delta))
            na.events.updated.fire()
        }

        rawMatrixAccumulator.forEach { (matrix, delta) ->
            matrix.add(config.optimizer.computeDelta(matrix, delta))
        }

        return error
    }

}