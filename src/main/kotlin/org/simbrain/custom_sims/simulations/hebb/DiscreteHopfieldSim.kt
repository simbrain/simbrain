package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.HopfieldTestConfig
import org.simbrain.custom_sims.simulations.hebb.createHopfieldTestPane
import org.simbrain.custom_sims.simulations.hebb.createPatternControlPanel
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.subnetworks.Hopfield.HopfieldUpdate
import org.simbrain.util.place
import org.simbrain.util.showNumericInputDialog

/**
 * Demo for studying discrete Hopfield networks,
 */

val discreteHopfieldSim = newSim {

    val numNeurons = showNumericInputDialog(message = "Number of neurons", initValue = 100) ?: return@newSim

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Hopfield network
    val hopfield = Hopfield(numNeurons).apply {
        updateFunc = HopfieldUpdate.SYNC
        customInfo.fontSize = 24
    }
    network.addNetworkModelAsync(hopfield)

    // Text to potentially integrate
    // Select an input pattern and click the train button on the Control panel to train the network on the selected pattern.
    // The model learns the pattern and “remembers” it. When randomizing the network (by clicking “N” [Neuron], “R” [Randomize], and “Space” [Iterate], or using “I” [Wand Mode] over the nodes), the network adjusts the nodes on each iteration to reconfigure the inputted pattern.
    // The Network remembers the pattern and the antipattern, and when iterating (“Space”), it iterates to recreate the pattern with the most similar nodes.
    // You can get the pattern to memorize all the different patterns and antipatterns by training each one, randomizing and iterating to see if it is remembered, and training that pattern again if it needs to be learned.
    addSidebarInfo(
        """ 
            # Discrete Hopfield
            
            [Hopfield networks](https://en.wikipedia.org/wiki/Hopfield_network) are recurrent networks often used for pattern recognition and to model memory 
            retrieval. In this simulation, you can test the network's ability to store and retrieve memories in the form of activation patterns.
            
            # What to Do         
            
            1. Select one of the four input patterns on the button panel. 
            
            2. Press the `Train on current pattern` button to train the network on that pattern. Each time you press it, it will "burn in" the pattern further where
            it reinforces that pattern into the network's "memory".
            
            3. Note that it will learn both the pattern and its anti-pattern.
            
            4. To confirm that the pattern is remembered, randomize the network by pressing `N -> R` and then iterating by pressing `Space` to see if the pattern is 
            recreated.
            
                - You can also manually create part of the pattern you trained the network and see if it can recreate it.
            
            ## Training on Multiple patterns
            
            Hopfield networks have a memory capacity of about `14%` of the number of nodes. In this case about `8` memories states. However those memories need to be 
            sufficiently distinct.  So the network should be able to learn all `6` patterns, but you must very carefully train it on them by clicking the pattern, 
            and then pressing `Train on current pattern` a certain number of times.
            
            ## Other things to observe
            
            When you iterate the network it tends to go to lower energy states.  
                
            # Credits
            
            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 249, 0, 509, 619)

        var numTrainIterations = 1

        createPatternControlPanel(hopfield.neuronGroup, false) {
            hopfield.randomize()
        }?.apply {
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train on current pattern") {
                with(network) { hopfield.trainOnCurrentPattern() }
            }
            val config = HopfieldTestConfig(
                workspace = workspace,
                hopfield = hopfield.neuronGroup,
                weights = hopfield.weightMatrix,
                applyTraining = { with(network) { hopfield.trainOnCurrentPattern()} },
                applyLearningRate = { hopfield.learningRate = it },
                applyReset = {
                    hopfield.clear()
                    hopfield.weightMatrix.hardClear()
                }
            )
            createHopfieldTestPane(config, true)
        }
    }

}