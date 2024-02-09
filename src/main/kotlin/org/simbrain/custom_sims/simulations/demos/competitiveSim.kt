package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

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
    competitive.inputLayer.setUpperBound(1.0)
    competitive.weights.randomize()

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

            addButton("Train") {
                competitive.inputLayer.activations = competitive.inputLayer.activations.add(NormalDistribution().sampleDouble(competitive.inputLayer.activations.size))
                workspace.iterate()
            }

        }
    }

}