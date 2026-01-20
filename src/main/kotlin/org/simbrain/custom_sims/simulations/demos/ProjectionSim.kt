package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.place
import org.simbrain.util.setSpectralRadius
import org.simbrain.util.showNumericInputDialog

/**
 * Create with an array-based recurrent network
 */
val recurrentNetArrayBased = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val numNeurons = showNumericInputDialog("Size of Neuron Array:", 100) ?:return@newSim
    val neuronArray = NeuronArray(numNeurons)
    val weightMatrix = WeightMatrix(neuronArray, neuronArray)
    weightMatrix.randomize()
    weightMatrix.weights.setSpectralRadius(.99) // for nicer dynamics
    network.addNetworkModelsAsync(listOf(neuronArray, weightMatrix))

    val projectionPlot = addProjectionPlot("Projection Plot").apply {
        projector.tolerance = .01
    }
    withGui {
        place(networkComponent, 0, 0, 500, 500)
        place(projectionPlot, 505, 5, 500, 500)
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        neuronArray couple projectionPlot
    }

    addSidebarInfo(
        """
            # Recurrent Network (Array Version)
            
            A neuron array linked to itself by a weight matrix. This simulation is used to study simple recurrent dynamics.   

            The basic thing you do in this simulation is press `run` in the desktop window and watch dynamics unfold.
             
            # What to Do
            
            Basic method to study dynamics in this model:
            
            - Randomize the neuron array by clicking on it and pressing `r`. This sets a new initial condition for the dynamics.
            - Randomize the weight matrix by clicking on it and pressing `r` to randomize. This creates a new dynamical system.
            - At any time you can press the `clear` button (the eraser) in the projection plot to start over with your plot.
            - You can press the `clamp` button in the plot, assuming PCA, to _freeze_ the current projection pattern.

            ## Other Things to Try
            
            - Right click on the weight matrix and view the eigenvalues and associate its behavior with the dominant eigenvalue.
            - Right click on the weight matrix and set the spectral radius. Values a little below `1` can produce interesting dynamics.
            - Double click on the neuron array and change the update rule, and see how this impacts the dynamics. 
            - Changing the type of projection, for example changing it to `Sammon` and pressing `play` to see an alternative way of projecting data.
            
            # Credits
            
            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )


}