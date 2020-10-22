package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.builders.sim
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.custom_sims.helper_classes.Simulation

/**
 * Initial work towards a Kotlin version of [Simulation].
 */
fun main() {

    val sample = sim {
        val neuron1 = neuron {
            activation = 10.5
            updateRule = DecayRule().apply { upperBound = 10.0 }
        }

        val neuron3 = neuron()

        val network = network {
            +neuron1
            val neuron2 = +neuron()
            +neuron3
            +(neuron1 connectTo neuron2) { strength = 10.0}
            +(neuron2 connectTo neuron1) {strength = 10.0}
        }

        // Objects can be treated as built within the sim context
        // After .run is called they are actually built
        task {
            setupNetwork(network) // TODO. This will be removed.
            network.iterate(100) {
                println("Neuron1: ${neuron1.activation}, Neuron3: ${neuron3.activation}")
                println("test: ${neuron1.self.upperBound}")
            }
        }


    }

    sample.run()

}