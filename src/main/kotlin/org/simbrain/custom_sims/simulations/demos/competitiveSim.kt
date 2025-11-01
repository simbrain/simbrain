package org.simbrain.custom_sims.simulations.demos

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
 * Demo for studying competitive networks.
 */
val competitiveSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val competitive = CompetitiveNetwork(7, 5)
    network.addNetworkModelAsync(competitive)
    competitive.inputLayer.setUpperBound(1.0)
    
    competitive.competitive.params.learningRate = 0.05

    var winningLabel = ""
    val labelToNodeMap = mutableMapOf<String, org.simbrain.network.core.Neuron>()

    val docViewer = addSidebarInfo(
        """
        # Introduction
        
        A simple competitive network is an unsupervised neural network trained to classify input patterns into output neurons. It learns to detect clusters in the input group, with the output responding to these clusters of patterns. The competitive group is initialized with randomized weights.

        One way to get familiar with this simulation is to treat it as a game. Try to train the network so that each input pattern triggers a distinct output neuron. This may be impossible, but you should at least be able to get four distinct responses.
        
        # Simulation Details
        
        The user selects different patterns using the buttons on the control panel. Each time a pattern is chosen and the network is iterated (stepped), the network trains a neuron to respond to the selected pattern. With each iteration the training algorithm is applied, strengthening the current response to the input.
        
        Some patterns have overlapping output neurons, making them more challenging to separately train. The pattern that activates the output neuron most will be the label of this output neuron, and the connection between the weight and neurons is strengthened.
        
        Over time, the output neurons improve their ability to classify the clusters in the input space. The user repeats this process until the trained network responds to each pattern with a different output neuron. The network relies on the statistical properties of the inputs provided during training.

        # Training Strategy
        
        Competitive networks can be challenging to train, especially with overlapping patterns. Here are key strategies for success:
        
        **Early Training:**
        - Train each pattern ONCE before repeating any pattern
        - This allows each competitive neuron to "claim" a pattern before they become too specialized
        - The order matters: try presenting all patterns once before cycling through them again
        
        **Avoid Overtraining:**
        - Don't train the same pattern multiple times in a row early on
        - This causes one neuron to become too dominant, leaving fewer neurons available for other patterns
        
        **If Training Fails:**
        - Use "Reset" to randomize weights and try again
        - Try a different presentation order
        - Right-click the competitive group to adjust learning rate (lower values like 0.02-0.05 work better for overlapping patterns)
        
        **Pattern Characteristics:**
        - Patterns with more unique features (non-overlapping input nodes) are easier to separate
        - Patterns that share many features will compete for the same neurons
        - The network uses normalized inputs, so patterns with similar proportions are harder to distinguish
        
        ## Advanced Parameters
        
        If you're having difficulty getting the network to separate patterns, try adjusting these parameters by right-clicking the competitive group:
        
        **Learning Rate:**
        - Default is `0.05`, which works well for most cases
        - Lower values (`0.01-0.03`): Slower but more stable learning, better for very similar patterns
        - Higher values (`0.1-0.2`): Faster learning but may be unstable or cause neurons to switch patterns
        
        **Leaky Learning:**
        - Enable `Use Leaky learning` to allow losing neurons to learn slowly
        - Set `Leaky learning rate` to about `0.01` (1/4 of main learning rate)
        - This helps prevent "dead" neurons that never win and improves coverage of the input space
        
        **Update Method:**
        - Default is `Rummelhart-Zipser`, which normalizes inputs and moves weights toward input patterns
        - Try `Alvarez-Squire` for an alternative algorithm that uses decay
        - If using Alvarez-Squire, set `Decay percent` to `0.001` or lower
        
        **Input Normalization:**
        - Default `Normalize inputs` is enabled, which divides inputs by their sum
        - This makes patterns with different numbers of active units harder to distinguish
        - Disable to preserve absolute magnitudes, which can help separate patterns like P4 and P5
        
        **Network Size:**
        - The simulation uses 5 competitive neurons for 5 patterns
        - Try increasing to 7 or 10 neurons to give the network more flexibility
        - More neurons means the network can find better representations
        
        # What to Do
        
        Try to train the network so that each input pattern triggers a distinct output neuron.
        
        1. Select a pattern using one of the buttons (Pattern 1-5)
        
        2. Click "Train" to iterate the network and see which output neuron wins
        
        3. Repeat with different patterns, training each one once before cycling
        
        4. Try to achieve a situation where each pattern activates a different output neuron
        
        5. Use "Reset" if you want to start over with fresh random weights
        
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 149, 4, 674, 615)
        createControlPanel("Control Panel",1, 4) {

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