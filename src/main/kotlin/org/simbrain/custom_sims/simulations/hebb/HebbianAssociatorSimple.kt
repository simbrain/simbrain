package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Synapse
import org.simbrain.network.synapse_update_rules.HebbianRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Demo for studying Hebbian Pattern association
 */
val hebbianAssociatorSimple = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val neuron1 = network.addNeuron {
        location = point(0, 0)
        label = "Neuron 1"
    }
    val neuron2 = network.addNeuron {
        location = point(0, 0)
        label = "Neuron 2"
    }
    val weight = Synapse(neuron1, neuron2, HebbianRule())
    network.addNetworkModels(neuron1, neuron2, weight)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }


}