package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.helper_classes.Simulation
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.util.addNeuron
import org.simbrain.network.util.addSynapse
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Initial work towards a Kotlin version of [Simulation].
 */
val testSim = newSim {

    val networkComponent = addNetworkComponent("Just a Neuron")

    val network = networkComponent.network

    val neuron1 = network.addNeuron {
        label = "I am a Neuron"
        location = point(100, 100)
    }

    val synapse1 = network.addSynapse(neuron1, neuron1)

    val strategy = AllToAll().apply {
        isSelfConnectionAllowed = false
    }

    println("Total Number of neurons: ${network.looseNeurons.size}")

    withGui {
        place(networkComponent) {
            location = point(250, 0)
        }
    }

}