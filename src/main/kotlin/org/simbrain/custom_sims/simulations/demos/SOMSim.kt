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
    network.addNetworkModel(SOM)
    SOM.inputLayer.setUpperBound(1.0)
    //SOM.weights.randomize()

    withGui {
        place(networkComponent, 139, 10, 868, 619)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Pattern 1") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.5, 1.0, 0.5, 0.0, 0.0, 0.0, 0.0)
            }
            addButton("Pattern 2") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.5, 1.0, 0.5, 0.0, 0.0, 0.0)
            }
            addButton("Pattern 3") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.5, 1.0, 0.5, 0.0, 0.0)
            }
            addButton("Pattern 4") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.0, 0.5, 1.0, 0.5, 0.0)
            }
            addButton("Pattern 5") {
                SOM.inputLayer.neuronList.activations =
                    listOf(0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 0.5)
            }
            addButton("Train") {
                SOM.inputLayer.activations = SOM.inputLayer.activations.add(NormalDistribution().sampleDouble(SOM.inputLayer.activations.size))
                workspace.iterateAsync()
            }
        }
    }
}