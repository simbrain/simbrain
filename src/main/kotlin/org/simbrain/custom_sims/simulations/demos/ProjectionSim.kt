package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.place

/**
 * Create with a neuron array and a projection
 */
val projectionSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val neuronArray = NeuronArray(25)
    val weightMatrix = WeightMatrix(neuronArray, neuronArray)
    weightMatrix.randomize()
    network.addNetworkModels(listOf(neuronArray, weightMatrix))

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 400, 400)
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Projection Plot")
    withGui {
        place(projectionPlot, 393, 5, 400, 400)
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        neuronArray couple projectionPlot
    }

    // Adding a docviewer
    val docViewer = addDocViewer(
        "Information",
        """
            # Projection demo
            In this demo simply run the simulations and observe how the network activations are projected. 
            Some things you can try:
            - Randomize the weight matrix by clicking on it and pressing "r" to randomize
            - Changing the type of projection, for example changing it to `Sammon` and pressing `play` to see an alternative way of projecting data
            - At any time you can press the `clear` button (the eraser) in the projection plot to start over with your plot        
        """.trimIndent()
    )
    withGui {
        place(docViewer, 784, 3, 400, 400)
    }

}