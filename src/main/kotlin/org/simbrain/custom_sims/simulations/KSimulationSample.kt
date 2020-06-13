package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.builders.sim
import org.simbrain.network.neuron_update_rules.DecayRule

fun main() {

    val sample = sim {
        val neuron1 = neuron {
            activation = 0.5
            updateRule = DecayRule()
        }

        val neuron3 = neuron()

        val network = network {
            +neuron1
            val neuron2 = +neuron()
            +neuron3
            +(neuron1 connectTo neuron2)
            +(neuron2 connectTo neuron3)
        }

        // Objects can be treated as built within the sim context
        // After .run is called they are actually built
        task {
            setupNetwork(network) // TODO. This will be removed.
            network.iterate(10) {
                println("Neuron1: ${neuron1.activation}, Neuron3: ${neuron3.activation}")
            }
        }

    }

    sample.run()

}