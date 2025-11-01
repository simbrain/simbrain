package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Demo for studying Competitive networks with ImageWorld,
 */
val competitiveGridSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val competitive = CompetitiveNetwork(100, 5)
    network.addNetworkModelAsync(competitive)
    competitive.inputLayer.setUpperBound(1.0)
    val inputs = competitive.inputLayer

    var winningLabel = ""
    val labelToNodeMap = mutableMapOf<String, org.simbrain.network.core.Neuron>()

    val docViewer = addSidebarInfo(
        """ 
            # Introduction
            
            A competitive network with a 10x10 grid of input neurons (100 total) competing to classify 5 visual patterns. This is similar to the `Competitive` simulation but with spatial input patterns arranged in a grid layout.
            
            # Simulation Details
            
            The network contains:
            - **Input Layer**: 100 neurons arranged in a 10x10 grid
            - **Competitive Layer**: 5 neurons that compete to represent the input patterns
            
            Each pattern is a 2D visual pattern with specific active pixels. The competitive neurons learn to respond to these patterns through winner-take-all dynamics and weight updates.
            
            # What to Do
            
            1. Select a pattern using one of the buttons (Pattern 1-5)
            
            2. Click `Train` to iterate the network and see which output neuron wins
            
            3. Repeat with different patterns, training each one once before cycling
            
            4. Try to achieve a situation where each pattern activates a different output neuron
            
            5. Use `Reset` if you want to start over with fresh random weights
            
            ## Training Tips
            
            - Train each pattern once before repeating any pattern (this helps neurons "claim" different patterns)
            - Avoid training the same pattern multiple times in a row early on
            - The `Add Noise` button adds small random variations to the current pattern
            
            For detailed training strategies and advanced parameter tuning, see the `Competitive` simulation documentation.
        
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 153, 10, 386, 619)
        createControlPanel("Control Panel", 0, 13) {

            addButton("Pattern 1") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                        0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0
                    )
                winningLabel = "P1"
            }
            addButton("Pattern 2") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0,
                        0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                        1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0
                    )
                winningLabel = "P2"
            }
            addButton("Pattern 3") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0
                    )
                winningLabel = "P3"
            }
            addButton("Pattern 4") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                    )
                winningLabel = "P4"
            }
            addButton("Pattern 5") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0,
                        0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                    )
                winningLabel = "P5"
            }

                addButton("Add Noise") {
                    competitive.inputLayer.activationArray = competitive.inputLayer.activationArray.add(NormalDistribution(standardDeviation = .01).sampleDouble(competitive.inputLayer.activationArray.size))
                }

            addButton("Train") {
                workspace.iterateSuspend()
                val winner = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it > 0.0 }]
                
                val previousNode = labelToNodeMap[winningLabel]
                if (previousNode != null && previousNode != winner) {
                    val oldLabel = previousNode.label ?: ""
                    previousNode.label = oldLabel.replace(winningLabel, "").replace(", ,", ",").trim(',', ' ').ifEmpty { null }
                }
                
                val currentLabel = winner.label
                if (currentLabel.isNullOrEmpty()) {
                    winner.label = winningLabel
                } else if (!currentLabel.contains(winningLabel)) {
                    winner.label = currentLabel + ", " + winningLabel
                }
                
                labelToNodeMap[winningLabel] = winner
            }
            addButton("Reset") {
                competitive.randomize()
                competitive.competitive.neuronList.forEach { it.label = null }
                labelToNodeMap.clear()
            }
        }
    }
}