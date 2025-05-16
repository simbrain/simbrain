package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.HopfieldTestConfig
import org.simbrain.custom_sims.simulations.hebb.createHopfieldTestPane
import org.simbrain.custom_sims.simulations.hebb.createPatternControlPanel
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.computeCorrelationMatrix
import org.simbrain.util.displayInDialog
import org.simbrain.util.place
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.widgets.MatrixPlot

/**
 * Demo for studying discrete Hopfield networks.
 *
 * Depending how this turns out might be able to merge to the other sim
 */

val hopfieldPatterns = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Hopfield network
    val hopfield = Hopfield(100)
    network.addNetworkModel(hopfield)

    addSidebarInfo(
        """ 
            # Introduction
            
            This demo allows you to study [Hopfield networks](https://en.wikipedia.org/wiki/Hopfield_network), which are recurent networks often used for pattern recognition and to model memory retrieval. 
            
            In this simulation you can compare how patterns that overlap more or less to see how many you get it to to learn, based on their correlations.

            # Training on One pattern         
            
            - Select one of the patterns on the button panel. 
            - Press the train button to train the network on that pattern. Each time you press "train" it will "burn in" the pattern further.
            - Note that it will learn both the pattern and its anti-pattern.
            - To confirm the pattern is remembered, try randomzing the network with `N -> R` and then iterating by pressing space to see if the pattern is recreated.
            - You can also manually create part of the pattern you trained the network and see if it can recreate it.
            
            # Other things to observe
            
            When you iterate the network it tends to go to lower energy states.      
        
        """.trimIndent()
    )

    // Patterns
    val uShape = DoubleArray(100) { 0.0 }
    for (i in 0 until 10) {
        uShape[i * 10] = 1.0         // Left column
        uShape[i * 10 + 9] = 1.0     // Right column
    }
    for (j in 0 until 10) {
        uShape[90 + j] = 1.0         // Bottom row
    }
    val diag = DoubleArray(100) { 0.0 }
    for (i in 0 until 10) {
        diag[i * 10 + i] = 1.0  // set diagonal element
    }

    withGui {
        place(networkComponent, 249, 0, 509, 619)

        createControlPanel("Control Panel", 0, 0) {
            addButton("U Pattern") {
                hopfield.neuronGroup.setActivations(uShape)
            }
            // Add more patterns here
            addButton("Diagonal Pattern") {
                hopfield.neuronGroup.setActivations(diag)
            }
            addSeparator()
            addButton("Train on current pattern") {
                with(network) { hopfield.trainOnCurrentPattern() }
            }
            addButton("Show correlations") {
                MatrixPlot(
                    listOf("U", "Diag"),
                    computeCorrelationMatrix(
                        arrayOf(
                            uShape,
                            diag
                        )
                    )
                ).displayInDialog().apply {
                    title = "Inter-pattern correlations"
                }
            }

        }
    }


}