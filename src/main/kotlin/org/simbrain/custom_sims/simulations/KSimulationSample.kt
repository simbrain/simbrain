package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.*
import org.simbrain.custom_sims.addImageWorld
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.helper_classes.Simulation
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.Neuron
import org.simbrain.network.layouts.HexagonalGridLayout
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
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    val imageWorldComponent = addImageWorld("Image World")
    withGui {
        place(imageWorldComponent) {
            location = point(410,0)
            width = 400
            height = 400
        }
    }

}

val linkedNeuronList = newSim {
    val networkComponent = addNetworkComponent("Neuron List")

    val network = networkComponent.network

    GlobalScope.launch(Dispatchers.Main) {
        val neurons = (1..10000).map {
            async(Dispatchers.Default) { Neuron(network) }
        }.awaitAll()

        neurons.forEach { network.addNetworkModel(it) }

        val synapses = with(network) {
            neurons.windowed(2) { (n1, n2) ->
                addSynapse(n1, n2) {
                    strength = 1.0
                }
            }
        }
        HexagonalGridLayout.layoutNeurons(neurons, 40, 40)
    }
}