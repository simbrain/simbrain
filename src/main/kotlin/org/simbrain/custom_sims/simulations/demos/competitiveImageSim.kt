package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Demo for studying Competitive networks with ImageWorld,
 */
val competitiveImageSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val competitive = CompetitiveNetwork(100, 5)
    network.addNetworkModel(competitive)
    competitive.inputLayer.setUpperBound(1.0)
    val inputs = competitive.inputLayer

    // Image world
    val component = addImageWorld("Image World")
    placeComponent(component,1200,100,500,405)
    val imageWorld = component.world
    imageWorld.resetImageAlbum(10, 10)
    imageWorld.setCurrentFilter("Threshold 10x10")

    // Couple
    with(couplingManager) {
        createCoupling(
            imageWorld.filterCollection.currentFilter.getProducer(imageWorld.filterCollection.currentFilter::getBrightness),
            inputs.getConsumer(inputs::activations))
    }

    // Label for winner
    var winningLabel = ""

    withGui {
        place(networkComponent, 139, 10, 868, 619)
        createControlPanel("Control Panel", 5, 10) {

            // Maybe we can add in patterns that meant to add more variance (excitation rather than inhibition) in the network's activation.
            // If so, use code below as reference.

//            addButton("Pattern 1") {
//                competitive.inputLayer.neuronList.activations =
//                    listOf(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0)
//                winningLabel = "P1"
//            }
//            addButton("Pattern 2") {
//                competitive.inputLayer.neuronList.activations =
//                    listOf(0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0)
//                winningLabel = "P2"
//            }
//            addButton("Pattern 3") {
//                competitive.inputLayer.neuronList.activations =
//                    listOf(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0)
//                winningLabel = "P3"
//            }
//            addButton("Pattern 4") {
//                competitive.inputLayer.neuronList.activations =
//                    listOf(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0)
//                winningLabel = "P4"
//            }
//            addButton("Pattern 5") {
//                competitive.inputLayer.neuronList.activations =
//                    listOf(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
//                winningLabel = "P5"
//            }

                addButton("Add Noise") {
                    competitive.inputLayer.activations = competitive.inputLayer.activations.add(NormalDistribution(standardDeviation = .01).sampleDouble(competitive.inputLayer.activations.size))
                }

            addButton("Train") {
                workspace.iterateSuspend()
                val winner = competitive.competitive.neuronList[competitive.competitive.activations.indexOfFirst { it > 0.0 }]
                winner.label = winningLabel
            }
        }
    }
}