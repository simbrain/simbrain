package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showOptionDialog
import org.simbrain.util.toMatrix
import smile.io.Read

/**
 * Train a smile classifier on Iris data.
 */
val irisClassifier = newSim {

    val option = showOptionDialog(
        "Choose Classifier Type",
        "Choose a classifier type to train on Iris data.",
        arrayOf("KNN", "Logistic Regression")
    )

    // Last column is target data
    val iris = Read.arff("simulations/tables/iris.arff")

    val classificationDataset = createClassificationDataset(
        dataFrame = iris,
        inputColumns = intArrayOf(0, 1, 2, 3),
        targetColumn = 4
    )

    val classifier = when (option) {
        0 -> KNNClassifier(classificationDataset)
        1 -> LogisticRegClassifier(classificationDataset)
        else -> return@newSim
    }

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val smileClassifier = ClassifierNetwork(classifier).apply {
        inputNeuronGroup.isAllClamped = true

        (outputNeuronGroup.layout as LineLayout).spacing = 100.0
        outputNeuronGroup.applyLayout()

        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
    }
    smileClassifier.train()

    // Set input data for iris to training data
    smileClassifier.inputNeuronGroup.inputData = classifier.trainingData.inputs.toMatrix()

    network.addNetworkModels(smileClassifier)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 800
            height = 500
        }
    }

}