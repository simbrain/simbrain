package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create with a neuron array and a projection
 */
val projectionSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val neuronArray = NeuronArray(network, 25)
    val weightMatrix = WeightMatrix(network, neuronArray, neuronArray)
    weightMatrix.randomize()
    network.addNetworkModelsAsync(listOf(neuronArray, weightMatrix))

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    withGui {
        place(projectionPlot) {
            location = point(410, 0)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        neuronArray couple projectionPlot
    }

}