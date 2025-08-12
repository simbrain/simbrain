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
            # Projection demo
            In this demo simply run the simulations and observe how the network activations are projected. 
            Some things you can try:
            - Randomize the weight matrix by clicking on it and pressing "r" to randomize
            - Changing the type of projection, for example changing it to `Sammon` and pressing `play` to see an alternative way of projecting data
            - At any time you can press the `clear` button (the eraser) in the projection plot to start over with your plot        
        """.trimIndent()
    )


}