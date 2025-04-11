package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.NakaRushtonRule
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.PCAProjection
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.stats.distributions.UniformRealDistribution
import javax.swing.JOptionPane

/**
 * Create with a recurrent neuron collection and a projection with a control panel to
 */
val recurrentProjection = newSim {

    val numNeurons = showNumericInputDialog("Number of Neurons:", 25)?:return@newSim

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val recurrentNet = network.addNeuronCollection(numNeurons) {
        upperBound = 100.0
        updateRule = NakaRushtonRule()
    }
    recurrentNet.label = "Network"
    recurrentNet.layout(GridLayout())
    network.addNetworkModel(recurrentNet)?.await()
    val wts = network.connect(recurrentNet.neuronList, recurrentNet.neuronList, Sparse().apply {
            connectionDensity = .15
        })

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    projectionPlot.projector.tolerance = .1
    projectionPlot.projector.projectionMethod = PCAProjection()

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        recurrentNet couple projectionPlot
    }

    withGui {
        place(networkComponent, 210, 0, 600, 600)
        place(projectionPlot, 810, 0, 600, 600)
        createControlPanel("Controls", 0, 0) {
            addButton("Randomize activations") {
                recurrentNet.randomize(UniformRealDistribution(0.0, 100.0))
            }
            addButton("Randomize Weights") {
                wts.forEach { it.randomize(UniformRealDistribution(-10.0, 10.0)) }
            }
            addButton("Clear Plot") {
                (getDesktopComponent(projectionPlot) as ProjectionDesktopComponent).clearData()
            }
        }
    }

    addSidebarInfo(
        """
            # Exploring Attractors in a Recurrent Neural Network
            
            ## Basic Definitions and Setup
            
            This simulation lets you explore how different recurrent neural networks evolve over time, forming patterns such as fixed points, limit cycles, or chaotic trajectories. 
            
            The system consists of a fully recurrent neuron population with randomly initialized connections. A **projection plot** visualizes the evolution of the network in a reduced 2D space using **Principal Component Analysis (PCA)**, making it easier to see the underlying dynamics.
            
            Key terms:
            
            - **Fixed Point:** A stable state the system settles into over time.
            - **Limit Cycle:** A repeating, periodic trajectory in activation space.
            - **Chaos:** Irregular, sensitive dynamics that never repeat and are hard to predict.
            - **Projection Plot (PCA):** A dimensionality-reduced visualization of the network's activity over time.
            
            To begin, just click **Run** in Simbrain's main toolbar. Everything else is handled through the custom control panel.
            
            ---
            
            ## Simulation Walkthrough
            
            1. **Start the Simulation**
               - Press **Run** in the Simbrain toolbar to begin updating the network.
            
            2. **Randomize Activations**
               - Click **"Randomize Activations"** to set random starting values for all neurons.
               - Observe the evolving trajectory in the projection plot.
               - Try this multiple times to explore different outcomes.
            
            3. **Clear Plot**
               - When the trajectory slows down or stabilizes, click **"Clear Plot"** to reset the display.
            
            4. **Randomize Weights**
               - Click **"Randomize Weights"** to create a new network with fresh, randomized connections.
               - This changes the system's dynamics—try again with randomized activations to see what new behavior emerges.
            
            ---
            
            ## Tips and Tricks
            
            - **Speed Up the Simulation**
              - Click inside the **Network window** and press the `5` key to toggle **weight visibility** off.
              - This reduces rendering load and makes the simulation run faster.
            
            - **Explore Different System Sizes**
              - When the simulation starts, you’ll be prompted to enter the **number of neurons**.
              - Try different values to see how system size affects dynamics.
            
            - **Types of Behavior to Look For**
              - **Fixed points**: Trajectory settles into a single location.
              - **Limit cycles**: Loops or rings in the projection plot.
              - **Chaos**: Irregular paths, often with sudden changes and no repeating pattern.
            
            You can experiment by repeatedly clicking the buttons in different sequences, observing the resulting behavior, 
            and thinking about how initial conditions and connection patterns shape neural dynamics.

            """.trimIndent()
    )

}