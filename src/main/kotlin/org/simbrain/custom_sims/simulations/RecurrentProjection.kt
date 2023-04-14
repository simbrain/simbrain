package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.PCAProjection2
import javax.swing.JOptionPane

/**
 * Create with a recurrent neuron collection and a projection with a control panel to
 */
val recurrentProjection = newSim {

    val numNeurons: Int = JOptionPane.showInputDialog(
        null,
        "Number of Neurons:",
        "25"
    ).toInt()

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val recurrentNet = network.addNeuronCollection(numNeurons)
    recurrentNet.label = "Network"
    recurrentNet.layout(GridLayout())
    recurrentNet.randomize()
    network.addNetworkModel(recurrentNet)
    Sparse().apply {
        connectionDensity = .25
    }.also {
        it.connectNeurons(network, recurrentNet.neuronList, recurrentNet.neuronList)
    }

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    projectionPlot.projector.tolerance = 2.0
    projectionPlot.projector.projectionMethod = PCAProjection2()
    withGui {
        place(projectionPlot) {
            location = point(410, 0)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        recurrentNet couple projectionPlot
    }

}