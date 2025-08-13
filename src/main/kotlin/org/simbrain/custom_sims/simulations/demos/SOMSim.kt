package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Demo for studying SOM networks,
 */
val SOMSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // SOM network
    val SOM = SOMNetwork(7, 16)
    network.addNetworkModelAsync(SOM)
    SOM.inputLayer.setUpperBound(1.0)
    //SOM.weights.randomize()

    // Label for winner
    var winningLabel = ""

    withGui {
        place(networkComponent, 139, 10, 868, 619)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Pattern 1") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.5, 1.0, 0.5, 0.0, 0.0, 0.0, 0.0)
                winningLabel = "P1"
            }
            addButton("Pattern 2") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.5, 1.0, 0.5, 0.0, 0.0, 0.0)
                winningLabel = "P2"
            }
            addButton("Pattern 3") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.5, 1.0, 0.5, 0.0, 0.0)
                winningLabel = "P3"
            }
            addButton("Pattern 4") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.0, 0.5, 1.0, 0.5, 0.0)
                winningLabel = "P4"
            }
            addButton("Pattern 5") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 0.5)
                winningLabel = "P5"
            }
            addButton("Train") {
                SOM.inputLayer.activationArray = SOM.inputLayer.activationArray.add(NormalDistribution().sampleDouble(SOM.inputLayer.activationArray.size))
                workspace.iterateSuspend()
                val winner = SOM.som.winner
                if (winner != null && winningLabel.isNotEmpty()) {
                    winner.label = winningLabel
                }
            }
        }
    }
}