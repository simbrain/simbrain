package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
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
    val labelTracker = WinnerLabeler()

    addSidebarInfo(
        """ 
            # Competitive Grid Network
            
            A competitive network with a `10`x`10` grid of input neurons (`100` total) competing to classify `5` visual patterns. This is similar to the `Competitive network (simple)` simulation but with spatial input patterns arranged in a grid layout.
            
            # Simulation Details
            
            The network contains:
            - Input Layer: `100` neurons arranged in a `10`x`10` grid
            - Competitive Layer: `5` neurons that compete to represent the input patterns
            
            Each pattern is a 2D visual pattern with specific active pixels. The competitive neurons learn to respond to these patterns through winner-take-all dynamics and weight updates.
            
            # What to Do
            
            1. Select a pattern using one of the buttons (Pattern `1-5`)
            
            2. Click `Train` to iterate the network and see which output neuron wins
            
            3. Repeat with different patterns, training each one once before cycling
            
            4. Try to achieve a situation where each pattern activates a different output neuron
            
            5. Use `Reset` if you want to start over with fresh random weights
            
            6. Use `Test` to see which neuron responds to the current pattern without updating weights (this is a shortcut to the `Clamp` menu item)
            
            ## Training Tips
            
            - Train each pattern once before repeating any pattern (this helps neurons "claim" different patterns)
            - Avoid training the same pattern multiple times in a row early on
            - The `Add Noise` button adds small random variations to the current pattern
            - If you're having trouble getting distinct responses, try right-clicking the competitive group and editing parameters. For example, turning off `Normalize inputs` sometimes improves results
            - We haven't fully explored all parameter combinations, and it's not entirely clear what settings produce the best results or what all the patterns of behavior are, so experimentation is encouraged
            
            ## Testing Advanced Features
            
            Right-click the competitive group to access advanced parameters. Here are some experiments to try:
            
            **Leaky Learning:** Enable `Use Leaky learning` and set rate to `0.01`. Train P1 ten times, then try other patterns. Compare with leaky learning disabled. You should see better pattern separation with it enabled.
            
            **Alvarez-Squire Method:** Change `Update method` to `Alvarez-Squire`. Train all patterns, then iterate without training. Watch weights gradually decay over time.
            
            **Normalization:** Disable `Normalize inputs`. Notice how patterns with more active pixels (like P2, the diamond) produce stronger responses.
            
            **Activation Dynamics:** Enable `Use activation dynamics` and `Add noise`. Watch the competitive neurons show varying activation levels instead of fixed 0/1 values.
            
            For detailed explanations, see the `Competitive network (simple)` simulation documentation. 
        
            # Credits
            
            Jasmine Lau
            
            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
            
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 153, 10, 528, 728)
        createControlPanel("Control Panel", 0, 13) {

            addButton("Pattern 1") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
                    )
                winningLabel = "P1"
            }
            addButton("Pattern 2") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0
                    )
                winningLabel = "P2"
            }
            addButton("Pattern 3") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0
                    )
                winningLabel = "P3"
            }
            addButton("Pattern 4") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0
                    )
                winningLabel = "P4"
            }
            addButton("Pattern 5") {
                competitive.inputLayer.neuronList.activations =
                    listOf(
                        0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0
                    )
                winningLabel = "P5"
            }

            addButton("Add Noise") {
                competitive.inputLayer.activationArray = competitive.inputLayer.activationArray.add(NormalDistribution(standardDeviation = .01).sampleDouble(competitive.inputLayer.activationArray.size))
            }
            
            addSeparator()
            
            addButton("Train") {
                workspace.iterateSuspend()
                val winner = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it > 0.0 }]
                labelTracker.updateWinner(winningLabel, winner)
            }
            addButton("Test") {
                val savedLearningRate = competitive.learningRate
                competitive.learningRate = 0.0
                workspace.iterateSuspend()
                competitive.learningRate = savedLearningRate
                val winner = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it > 0.0 }]
                labelTracker.updateWinner(winningLabel, winner)
            }
            addButton("Reset") {
                competitive.randomize()
                labelTracker.clear(competitive.competitive.neuronList)
            }
        }
    }
}