package org.simbrain.network.trainers

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.SoftmaxRule

/**
 * Interface for networks that can be trained using standard supervised learning methods.
 */
interface SupervisedNetwork {

    val trainerConfig: SupervisedTrainerConfig

    var trainingSet: MatrixDataset

    var testingSet: MatrixDataset

    val inputLayer: Layer

    val outputLayer: Layer

    val layers: LinkedHashSet<Layer>

    context(Network) fun forwardPass()

    fun initWeights()

    fun initBiases()

    fun possibleLossFunctions() = when((outputLayer as? NeuronArray)?.updateRule) {
        is SoftmaxRule -> listOf(BackpropLossFunction.CrossEntropy::class.java)
        else -> listOf(BackpropLossFunction.SSE::class.java, BackpropLossFunction.MSE::class.java, BackpropLossFunction.RMSE::class.java)
    }

}