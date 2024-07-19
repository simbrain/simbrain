package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Demo for studying competitive networks,
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

    // Label for winner
    var winningLabel = ""

    withGui {
        place(networkComponent, 139, 10, 868, 619)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Pattern 1") {
                competitive.inputLayer.neuronList.activations =
                    listOf(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0)
                winningLabel = "P1"
            }
            addButton("Pattern 2") {
                competitive.inputLayer.neuronList.activations =
                    listOf(0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0)
                winningLabel = "P2"
            }
            addButton("Pattern 3") {
                competitive.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0)
                winningLabel = "P3"
            }
            addButton("Pattern 4") {
                competitive.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0)
                winningLabel = "P4"
            }
            addButton("Pattern 5") {
                competitive.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                winningLabel = "P5"
            }
            addButton("Add Noise") {
                competitive.inputLayer.activationArray = competitive.inputLayer.activationArray.add(NormalDistribution(standardDeviation = .01).sampleDouble(competitive.inputLayer.activationArray.size))
            }
            addButton("Train") {
                workspace.iterateSuspend()
                val winner = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it > 0.0 }]
                winner.label = winningLabel
            }
        }
    }
}