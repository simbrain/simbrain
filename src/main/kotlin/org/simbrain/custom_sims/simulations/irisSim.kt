package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.util.place
import org.simbrain.util.point
import smile.io.Read

/**
 * Train a smile classifier on Iris data.
 */
val irisClassifier = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Last column is target data
    val iris = Read.arff("simulations/tables/iris.arff")

    // Add a neuron collection for setting inputs to the network
    val inputNc = network.addNeuronCollection(4)
    inputNc.label = "Inputs"
    inputNc.setClamped(true)
    inputNc.location = point(0, 0)

    // Choose a classifier here
    // val classifier = LogisticRegClassifier(4, 3)
    // val classifier = SVMClassifier(4, 3)
    val classifier = KNNClassifier(4, 3)

    classifier.trainingData.featureVectors = iris.select(0,1,2,3).toArray()
    classifier.trainingData.targetLabels = iris.column(4).toStringArray()
    inputNc.inputManager.data = classifier.trainingData.featureVectors
    val smileClassifier = SmileClassifier(network, classifier)
    smileClassifier.train()

    val weightMatrix = WeightMatrix(network, inputNc, smileClassifier)
    network.addNetworkModels(weightMatrix, smileClassifier)
    smileClassifier.location = point(0, -300)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 800
            height = 500
        }
    }

}