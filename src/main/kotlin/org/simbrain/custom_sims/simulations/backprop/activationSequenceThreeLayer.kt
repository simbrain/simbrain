package org.simbrain.custom_sims.simulations.backprop

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.util.point
import org.simbrain.util.toMatrix

val activationSequenceThreeLayer = newSim {

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
    output.location = point(100, 100)
    hidden.location = point(100, 500)
    inputs.location = point(100, 900)

    val inputData = arrayOf(
        doubleArrayOf(1.0, 0.0, 1.0, 0.0, 1.0, 0.0),
        doubleArrayOf(0.0, 1.0, 0.0, 0.0, 1.0, 0.0),
    ).toMatrix()

    val targetData = arrayOf(
        doubleArrayOf(1.0, 1.0, 1.0),
        doubleArrayOf(0.0, 2.0, 0.0)
    ).toMatrix()

    model.trainingSet = MatrixDataset(
        inputs = inputData,
        targets = targetData
    )


}