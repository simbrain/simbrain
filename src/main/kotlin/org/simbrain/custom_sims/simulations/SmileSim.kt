package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.createNeuronCollection
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.table.DataFrameWrapper
import smile.io.Read

/**
 * Simulation for evaluating and demoing the Smile ML components
 */
val smileSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Last column is target data
    val data = DataFrameWrapper(Read.csv("simulations/tables/iris_combined.csv"))

    // Add a neuron collection for setting inputs to the network
    val inputNc = network.createNeuronCollection(data.columnCount - 1)
    inputNc.label = "Inputs"
    inputNc.setClamped(true)
    inputNc.location = point(0, 0)

    val lr = LogisticRegClassifier(4, 3)
    val inputs = data.get2DDoubleArray(0 until data.columnCount - 1)
    val targets = data.getIntColumn(data.columnCount - 1)

    inputNc.inputManager.data = inputs

    val smileClassifier = SmileClassifier(network, lr)
    lr.fit(inputs, targets)

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