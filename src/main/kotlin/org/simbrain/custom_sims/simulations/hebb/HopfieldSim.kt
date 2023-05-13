package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Demo for studying Hopfield networks,
 */
val hopfieldSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Hopfield network
    val hopfield = Hopfield(network, 64)
    network.addNetworkModel(hopfield)

    withGui {
        place(networkComponent) {
            location = point(280, 10)
            width = 400
            height = 400
        }
        createControlPanel("Control Panel", 5, 10) {

            addButton("Stub") {
            }
            addTextField("Stub", "" + .01) {
            }
        }
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    withGui {
        place(projectionPlot) {
            location = point(667, 10)
            width = 400
            height = 400
        }
    }

    // Couple the network to the projection plot
    with(couplingManager) {
        hopfield.neuronGroup couple projectionPlot
    }

}