package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.synapse_update_rules.HebbianRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Demo for studying Hebbian Pattern association
 */
val hebbianAssociator = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val recurrentNet = network.addNeuronCollection(25).apply {
        label = "Neurons"
        layout(GridLayout(40.0, 40.0))
    }
    network.addNetworkModel(recurrentNet)
    network.connect(recurrentNet.neuronList, recurrentNet.neuronList, Sparse().apply {
        connectionDensity = .15
    }).forEach { s ->
        s.learningRule = HebbianRule().apply {
            learningRate = .01
        }
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
    withGui {
        place(projectionPlot) {
            location = point(410, 0)
            width = 400
            height = 400
        }
    }

    // Couple the network to the projection plot
    with(couplingManager) {
        recurrentNet couple projectionPlot
    }

}