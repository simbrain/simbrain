package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addDocViewer
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.place

/**
 * Demo for studying Hopfield networks,
 * Console 5 (0, 0, 418, 622)
 * Control Panel (469, -2, 113, 275)
 * Network (624, 0, 591, 667)
 */
val hopfieldSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Hopfield network
    val hopfield = Hopfield(64)
    network.addNetworkModel(hopfield)

    withGui {
        place(networkComponent, 139, 10, 683, 624)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Pattern 1") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0)

            }

            addButton("Pattern 2") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0)

            }

            addButton("Pattern 3") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0)

            }

            addButton("Pattern 4") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0)

            }

            addButton("Pattern 5") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0)

            }

            addButton("Pattern 6") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

            }

            addButton("Train") {
                hopfield.trainOnCurrentPattern()

            }

        }
    }

    val docViewer = addDocViewer(
        "Hopfield Network",
        """ 
            # Introduction
            
            [Hopfield networks](https://en.wikipedia.org/wiki/Hopfield_network) are recurent networks often used for pattern recognition and to model memory retrieval. In this simulation you can test the network's ability to store patterns
            
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
        place(docViewer, 812, 10, 542, 627)
    }

    // // Location of the projection in the desktop
    // val projectionPlot = addProjectionPlot2("Activations")
    // withGui {
    //     place(projectionPlot) {
    //         location = point(667, 10)
    //         width = 400
    //         height = 400
    //     }
    // }
    //
    // // Couple the network to the projection plot
    // with(couplingManager) {
    //     hopfield.neuronGroup couple projectionPlot
    // }

}