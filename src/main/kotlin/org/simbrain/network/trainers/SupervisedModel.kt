package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.events.LocationEvents
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.rowVectorTransposed
import java.awt.geom.Point2D

class SupervisedModel(
    override val inputLayer: NeuronArray,
    override val outputLayer: NeuronArray
): LocatableModel(), SupervisedNetwork {

    val weightMatrixTree: WeightMatrixTree = WeightMatrixTree(listOf(inputLayer), outputLayer)

    val layers = weightMatrixTree.tree.flatten().flatMap { listOf(it.tar, it.src) }.distinct()

    @Transient
    override val events = LocationEvents()

    override val trainer = SupervisedModelTrainer()

    override var trainingSet: MatrixDataset = MatrixDataset(
        inputLayer.activations.transpose(),
        outputLayer.activations.transpose()
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

    override fun initWeights() {
        weightMatrixTree.tree.flatten().forEach { trainer.weightInitializationStrategy.initializeWeights(it) }
    }

    override fun initBiases() {
        weightMatrixTree.tree.flatten().map { it.tar }.distinct().forEach {
            it.clear()
            it.randomizeBiases()
        }
    }

    context(Network)
    override fun forwardPass() {
        weightMatrixTree.forwardPass(listOf(inputLayer.activations))
    }

    override suspend fun delete() {
        events.deleted.fire(this).await()
    }
}

class SupervisedModelTrainer: SupervisedTrainer<SupervisedModel>() {

    context(Network)
    override fun SupervisedModel.trainRow(rowNum: Int): Double {
        inputLayer.setActivations(trainingSet.inputs.row(rowNum))
        val targetVec = trainingSet.targets.rowVectorTransposed(rowNum)
        weightMatrixTree.forwardPass(listOf(inputLayer.activations))
        return weightMatrixTree.applyBackprop(targetVec, epsilon = learningRate, lossFunction = lossFunction)
    }

}