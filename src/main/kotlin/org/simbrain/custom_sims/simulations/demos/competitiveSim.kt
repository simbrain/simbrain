package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
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
    competitive.applySimulationLayout()
    competitive.inputLayer.setUpperBound(1.0)
    
    competitive.learningRate = 0.05

    var winningLabel = ""
    val labelTracker = WinnerLabeler()

    addSidebarInfo(
        """
        # Simple Competitive Network
        
        A simple competitive network is an unsupervised neural network trained to classify input patterns into output neurons. It learns to detect clusters in the input group, with the output responding to these clusters of patterns. The competitive group is initialized with randomized weights.

        One way to get familiar with this simulation is to treat it as a game. Try to train the network so that each input pattern triggers a distinct output neuron. This may be impossible, but you should at least be able to get four distinct responses. If you're having trouble, try right-clicking the competitive group and editing parameters. For example, turning off `Normalize inputs` sometimes improves results. We haven't fully explored all parameter combinations, and it's not entirely clear what settings produce the best results or what all the patterns of behavior are, so experimentation is encouraged.
        
        # Simulation Details
        
        The user selects different patterns using the buttons on the control panel. Each time a pattern is chosen and the network is iterated (stepped), the network trains a neuron to respond to the selected pattern. With each iteration the training algorithm is applied, strengthening the current response to the input.
        
        Some patterns have overlapping output neurons, making them more challenging to separately train. The pattern that activates the output neuron most will be the label of this output neuron, and the connection between the weight and neurons is strengthened.
        
        Over time, the output neurons improve their ability to classify the clusters in the input space. The user repeats this process until the trained network responds to each pattern with a different output neuron. The network relies on the statistical properties of the inputs provided during training.

        ## Training Strategy
        
        Competitive networks can be challenging to train, especially with overlapping patterns. Here are key strategies for success:
        
        Early Training:
        - Train each pattern ONCE before repeating any pattern
        - This allows each competitive neuron to "claim" a pattern before they become too specialized
        - The order matters: try presenting all patterns once before cycling through them again
        
        Avoid Overtraining:
        - Don't train the same pattern multiple times in a row early on
        - This causes one neuron to become too dominant, leaving fewer neurons available for other patterns
        
        If Training Fails:
        - Use `Reset` to randomize weights and try again
        - Try a different presentation order
        - Right-click the competitive group to adjust learning rate (lower values like `0.02` to `0.05` work better for overlapping patterns)
        
        Testing Performance:
        - To test the network without further learning, turn off weight updates by selecting the synapse group (click on the weights) and choosing `Clamp` from the right-click menu, or by right-clicking on the synapse group node and selecting `Clamp all`
        - This "freezes" the weights so you can test pattern recognition without changing what the network has learned
        - Unclamp to resume training
        
        Pattern Characteristics:
        - Patterns with more unique features (non-overlapping input nodes) are easier to separate
        - Patterns that share many features will compete for the same neurons
        - The network uses normalized inputs, so patterns with similar proportions are harder to distinguish
        
        ## Advanced Parameters
        
        If you're having difficulty getting the network to separate patterns, try adjusting these parameters by right-clicking the competitive group:
        
        `Learning Rate`:
        - Default is `0.05`, which works well for most cases
        - Lower values (`0.01` to `0.03`): Slower but more stable learning, better for very similar patterns
        - Higher values (`0.1` to `0.2`): Faster learning but may be unstable or cause neurons to switch patterns
        
        `Leaky Learning` (Prevents Dead Neurons):
        - Enable `Use Leaky learning` to allow losing neurons to learn slowly
        - Set `Leaky learning rate` to about `0.01` (`1/4` of main learning rate)
        - What to observe: Without leaky learning, some neurons may never win. With it enabled, all neurons should eventually participate
        - Test it: Train on P1 repeatedly (`10` times), then try P2 through P5. Without leaky learning, you may find some patterns can't find a neuron
        
        `Update Method` (Different Learning Algorithms):
        - Default is `Rummelhart-Zipser`, which normalizes inputs and moves weights toward input patterns
        - Try `Alvarez-Squire` for an alternative algorithm with weight decay (models memory consolidation)
        - What to observe: Alvarez-Squire causes weights to gradually decay, so patterns need periodic retraining
        - Test it: Switch to Alvarez-Squire, train all patterns once, then wait (iterate without training). Weights will slowly decay
        - If using Alvarez-Squire, set `Decay percent` to `0.001` or lower
        
        `Input Normalization` (Scale Invariance):
        - Default `Normalize inputs` is enabled, which divides inputs by their sum
        - This makes patterns with different numbers of active units harder to distinguish
        - What to observe: With normalization, P1 (3 active) and P5 (3 active) are treated similarly despite different positions
        - Test it: Disable normalization and train again. Patterns with more active inputs will produce stronger responses
        - Disable to preserve absolute magnitudes, which can help separate patterns like P4 and P5
        
        `Activation Dynamics` (Biological Realism):
        - Enable `Use activation dynamics` for more realistic neuron behavior with decay
        - Set `Activation decay` to `0.7` (winner activation decays over time)
        - Enable `Add noise` to inject random fluctuations in winner activation
        - What to observe: Winner activations become more variable and dynamic rather than fixed at `1.0`
        - Test it: Enable both options and watch the competitive neurons. They'll show varying activation levels
        
        `Network Size`:
        - The simulation uses `5` competitive neurons for `5` patterns
        - Try increasing to `7` or `10` neurons to give the network more flexibility
        - More neurons means the network can find better representations
        
        # What to Do
        
        Try to train the network so that each input pattern triggers a distinct output neuron.
        
        1. Select a pattern using one of the buttons (Pattern `1-5`)
        
        2. Click `Train` to iterate the network and see which output neuron wins
        
        3. Repeat with different patterns, training each one once before cycling
        
        4. Try to achieve a situation where each pattern activates a different output neuron
        
        5. Use `Reset` if you want to start over with fresh random weights
        
        6. Use `Test` to see which neuron responds to the current pattern without updating weights (this is a shortcut to the `Clamp` menu item)
        
        # Credits
        
        Jasmine Lau
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
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
