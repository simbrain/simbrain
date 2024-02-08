package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.place

/**
 * Demo for studying Competitive networks,
 */
val competitiveSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val competitive = CompetitiveNetwork(7, 5)
    network.addNetworkModel(competitive)

    withGui {
        place(networkComponent, 139, 10, 868, 619)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Pattern 1") {
                competitive.inputLayer.neuronList.activations =
                    listOf(1.0, 0.5, 0.0, 0.0, 1.0, 0.5, 0.5)

            }

            addButton("Pattern 2") {
                competitive.inputLayer.neuronList.activations =
                    listOf(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5)

            }

            addButton("Pattern 3") {
                competitive.inputLayer.neuronList.activations =
                    listOf(1.0, 0.0, 0.5, 1.0, 1.0, 0.0, 0.0)

            }

            addButton("Pattern 4") {
                competitive.inputLayer.neuronList.activations =
                    listOf(1.0, 1.0, 0.5, 0.0, 0.0, 0.0, 0.5)

            }

            addButton("Pattern 5") {
                competitive.inputLayer.neuronList.activations =
                    listOf(0.5, 1.0, 0.0, 0.5, 0.0, 0.5, 0.0)

            }

            //var = getOneHotArray(4, 5).toList()

            addButton("Train") {
               workspace.iterate()

            }

        }
    }

    // // Location of the projection in the desktop
    // val projectionPlot = addProjectionPlot2("Activations")
    // withGui {
    //     place(projectionPlot) {
    //         location = point(667, 10)
    //         width = 400
    //         height = 400
    //     }
    // }
    //
    // // Couple the network to the projection plot
    // with(couplingManager) {
    //     hopfield.neuronGroup couple projectionPlot
    // }

}