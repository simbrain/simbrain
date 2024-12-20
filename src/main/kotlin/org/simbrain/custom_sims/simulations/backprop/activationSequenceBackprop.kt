package org.simbrain.custom_sims.simulations.backprop

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.toMatrix

val activationSequenceBackprop = newSim {

    val sequenceSize = 2
    val inputSize = 3
    val hiddenSize = 2

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val inputs = ActivationSequence(sequenceSize, inputSize).apply {
        label = "Inputs"
        isClamped = true
    }

    val hidden = ActivationSequence(sequenceSize, hiddenSize).apply {
        label = "Hidden"
    }

    val output = NeuronArray(inputSize).apply {
        label = "Output"
    }

    val wm1 = WeightMatrix(inputs, hidden)
    val wm2 = WeightMatrix(hidden, output)

    wm1.randomize()
    wm2.randomize()

    val model = SupervisedModel(inputs, output, false).apply {
        trainer.testConfiguration.enabled = false
    }

    network.addNetworkModels(inputs, hidden, output, wm1, wm2, model).awaitAll()

    offsetNetworkModel(inputs, hidden, Direction.NORTH, 200.0)
    offsetNetworkModel(hidden, output, Direction.NORTH, 200.0)

    val inputData = arrayOf(
        doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6),
        doubleArrayOf(-.1, -.2, -.3, 0.4, 0.5, 0.6),
        doubleArrayOf(0.1, 0.1, 0.1, 0.1, 0.1, 0.1),
    ).toMatrix()

    val targetData = arrayOf(
        doubleArrayOf(0.5, 0.7, 0.9),
        doubleArrayOf(0.3, 0.3, 0.3),
        doubleArrayOf(0.2, 0.2, 0.2),
    ).toMatrix()

    model.trainingSet = MatrixDataset(
        inputs = inputData,
        targets = targetData
    )


}