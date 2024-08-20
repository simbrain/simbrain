package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addDocViewer
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

    val docViewer = addDocViewer(
        "Information",
        """
            # Competitive Network (Simple) Overview: 
            
            A simple competitive network is an unsupervised neural network trained to classify input patterns into output neurons. It learns to detect clusters in the input group, with the output responding to these clusters of patterns.
            The competitive group is initialized with randomized weights. 

            # What to Do: 
        
            The user selects different patterns using the buttons on the control panel. Each time a pattern is chosen and the network is iterated (stepped), the network trains a neuron to respond to the selected pattern. With each iteration the training algorithm is applied, strengthening the current response to the input.
            Some patterns have overlapping output neurons, making them more challenging to separately train. The pattern that activates the output neuron most will be the label of this output neuron, and the connection between the weight and neurons is strengthened. Over time, the output neurons improve their ability to classify the clusters in the input space. 
            The user repeats this process until the trained network responds to each pattern with a different output neuron. The network relies on the statistical properties of the inputs provided during training. 
            
            One way to get familiar with this simulation is to treat it as a game. Try to train the network so that each input pattern triggers a distinct output neuron, associating each input to a distinct output.  This is not easy, since overlapping inputs often get mapped to the same output. So it requires using different inputs early on, and training just once per different input.  Overtraining an input can cause it to be over-represented.
            
        """.trimIndent()
    )

    withGui {
        place(docViewer, 0, 0,  464, 674)
        place(networkComponent, 661, 0, 674, 619)
        createControlPanel("Control Panel", 510, 0) {

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