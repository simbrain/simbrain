package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.place

/**
 * Create with a neuron array and a projection
 */
val projectionSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val neuronArray = NeuronArray(25)
    val weightMatrix = WeightMatrix(neuronArray, neuronArray)
    weightMatrix.randomize()
    network.addNetworkModelsAsync(listOf(neuronArray, weightMatrix))

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 400, 400)
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    withGui {
        place(projectionPlot, 405, 0, 400, 400)
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        neuronArray couple projectionPlot
    }

}