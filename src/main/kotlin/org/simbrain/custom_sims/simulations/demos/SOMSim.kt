package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
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

    var winningLabel = ""
    val labelTracker = WinnerLabeler()

    withGui {
        place(networkComponent, 157, 10, 548, 750)
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

            addButton("Add Noise") {
                SOM.inputLayer.activationArray = SOM.inputLayer.activationArray.add(NormalDistribution(standardDeviation = .01).sampleDouble(SOM.inputLayer.activationArray.size))
            }
            
            addSeparator()
            
            addButton("Train") {
                workspace.iterateSuspend()
                val winner = SOM.som.winner
                if (winner != null) {
                    labelTracker.updateWinner(winningLabel, winner)
                }
            }
            addButton("Test") {
                val savedLearningRate = SOM.som.learningRate
                val savedNeighborhoodSize = SOM.som.neighborhoodSize
                SOM.som.learningRate = 0.0
                workspace.iterateSuspend()
                SOM.som.learningRate = savedLearningRate
                SOM.som.neighborhoodSize = savedNeighborhoodSize
                val winner = SOM.som.winner
                if (winner != null) {
                    labelTracker.updateWinner(winningLabel, winner)
                }
            }
            addButton("Reset") {
                SOM.randomize()
                SOM.som.reset()
                labelTracker.clear(SOM.som.neuronList)
            }
        }
    }

    addSidebarInfo(
        """
        # Self Organizing Map (SOM)
        
        This simulation demonstrates a Self Organizing Map (SOM), also known as a Kohonen map. SOMs are unsupervised neural 
        networks that learn to create topologically ordered representations of input data. They organize similar inputs 
        to be mapped to nearby locations in the output space.
        
        SOMs implement competitive learning where neurons compete to respond to inputs. The winner and its spatial 
        neighbors update their weights toward the input, creating smooth topological maps where similar inputs map to 
        nearby outputs.

        # Simulation Details
        
        During training, the SOM learns to map similar input patterns to nearby locations in the 2D grid. 
        The "winning" neuron (most active) and its neighbors adjust their weights to become more responsive to the current input pattern.
        
        As the network learns you can observe the spatial range of learning and the learning rate reduce. You can right 
        click in the network interaction box to reset this process.

        # What to Do
        
        1. Select input patterns using the Pattern 1-5 buttons to present different input patterns to the network
        
        2. Train the network by clicking the `Train` button after selecting a pattern. This updates the weights based on the current input
        
        3. Observe the organization. After training with different patterns, similar patterns (which you can invoke with the buttons on the contorl panel, or manually by editing the inputs) should activate neurons in nearby locations
        
        4. Experiment with different training sequences:
           - Train with the same pattern multiple times to see how it becomes more strongly represented
           - Alternate between different patterns to see how the map organizes
           - Try training patterns in different orders
        
        5. Watch the winner labels. The winning neuron gets labeled with the pattern name, helping you see the organization
        
        6. Use `Test` to see which neuron responds to the current pattern without updating weights or decaying learning parameters (this is a shortcut to easily-accessed menu items)
        
        7. Use the `Recall SOM Pattern` menu item (right-click on the SOM layer) to see what input pattern a specific output neuron is currently tuned to. This pushes the weights of the selected neuron back to the input layer

        # References
        
        Kohonen, T. (1982). [Self-organized formation of topologically correct feature maps](https://doi.org/10.1007/BF00337288). _Biological Cybernetics_, _43_(1), 59-69.

        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )
}