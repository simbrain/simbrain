package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addProjectionPlot
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

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Network")

    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val na = NeuronArray(network, 25)
    val wm = WeightMatrix(network, na, na)
    network.addNetworkModels(listOf(na, wm))

    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    val projectionPlot = addProjectionPlot("Activations")

    withGui {
        place(projectionPlot) {
            location = point(410, 0)
            width = 400
            height = 400
        }
    }

    with(couplingManager) {
        na couple projectionPlot
    }

}