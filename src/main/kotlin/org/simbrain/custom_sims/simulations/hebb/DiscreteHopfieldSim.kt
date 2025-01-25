package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.hebb.*
import org.simbrain.network.subnetworks.Hopfield
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
    val hopfield = Hopfield(numNeurons)
    network.addNetworkModel(hopfield)

    // Text to potentially integrate
    // Select an input pattern and click the train button on the Control panel to train the network on the selected pattern.
    // The model learns the pattern and “remembers” it. When randomizing the network (by clicking “N” [Neuron], “R” [Randomize], and “Space” [Iterate], or using “I” [Wand Mode] over the nodes), the network adjusts the nodes on each iteration to reconfigure the inputted pattern.
    // The Network remembers the pattern and the antipattern, and when iterating (“Space”), it iterates to recreate the pattern with the most similar nodes.
    // You can get the pattern to memorize all the different patterns and antipatterns by training each one, randomizing and iterating to see if it is remembered, and training that pattern again if it needs to be learned.
    addSidebarInfo(
        """ 
            # Introduction
            
            [Hopfield networks](https://en.wikipedia.org/wiki/Hopfield_network) are recurent networks often used for pattern recognition and to model memory retrieval. 
            In this simulation you can test the network's ability to store and retrieve memories in the form of activation patterns.

            # Training on One pattern         
            
            - Select one of the six training patterns on the button panel. 
            - Press the train button to train the network on that pattern. Each time you press "train" it will "burn in" the pattern further.
            - Note that it will learn both the pattern and its anti-pattern.
            - To confirm the pattern is remembered, try randomzing the network with `N -> R` and then iterating by pressing space to see if the pattern is recreated.
            - You can also manually create part of the pattern you trained the network and see if it can recreate it.
            
            # Training on Multiple patterns
            
            - Hopfield networks have a memory capacity of about 14% of the number of nodes. In this case about 8 memories. However those memories need to be sufficiently distinct.  So the network should be able to learn all 6 patterns, but you must very carefully train it on them, clickin the pattern, and then pressing train a certain number of times.
            
            # Other things to observe
            
            When you iterate the network it tends to go to lower energy states.      
        
        """.trimIndent()
    )


    withGui {
        place(networkComponent, 228, 0, 509, 619)

        var numTrainIterations = 1
        fun trainingMode() {
            hopfield.neuronGroup.isAllClamped = true
            hopfield.synapseGroup.synapses.forEach{it.clamped = true}
        }
        fun retrievalMode() {
            hopfield.neuronGroup.isAllClamped = false
            hopfield.synapseGroup.synapses.forEach{it.clamped = false}
        }

        createPatternControlPanel(hopfield.neuronGroup, false)?.apply {
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train On All Patterns") {
                with(network) {
                    hopfield.randomize()
                    trainingMode()
                    repeat(numTrainIterations) {
                        applyCirclePattern(hopfield.neuronGroup)
                        hopfield.trainOnCurrentPattern()
                        applySquarePattern(hopfield.neuronGroup)
                        hopfield.trainOnCurrentPattern()
                        applyLinePattern(hopfield.neuronGroup, "diagonal")
                        hopfield.trainOnCurrentPattern()
                        applyCrossPattern(hopfield.neuronGroup)
                        hopfield.trainOnCurrentPattern()
                    }
                    // Dump into retrieval mode for easy testing
                    retrievalMode()
                }
            }
            addSeparator()
            addButton("Train on current pattern") {
                with(network) { hopfield.trainOnCurrentPattern() }
            }
            addSeparator()
            addButton("Training Mode") {
                trainingMode()
            }
            addButton("Retrieval Mode") {
                retrievalMode()
            }
        }
    }

}