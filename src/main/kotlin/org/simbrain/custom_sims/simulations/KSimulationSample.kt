package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.builders.KSimulation
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.network.synapse_update_rules.HebbianRule

fun main() {

    KSimulation().apply {
        val neuron1 = neuron {
            activation = 0.5
            updateRule = DecayRule()
        }

        val network = network {
            +neuron1
            val neuron2 = +neuron()
            (neuron1 connectTo neuron2) {
                learningRule = HebbianRule()
            }
        }

        // Objects can be treated as built within the sim context
        // After .run is called they are actually built
        sim {
            setupNetwork(network) // TODO. This will be removed.
            network.iterate(10) {
                println(neuron1.self)
                neuron1.self.update()
            }
        }
    }.run()

}