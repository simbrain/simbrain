package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.simbrain.network.core.Network
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.util.complement

fun assertStrategiesPatterns(
    network: Network,
    strategy1: ConnectionStrategy,
    strategy2: ConnectionStrategy,
    neuronCount: Int = 25,
    expectIdentical: Boolean = true
) {
    runBlocking {
        with(network) {
            val neurons1 = addNeuronCollection(neuronCount).neuronList.mapIndexed { index, neuron -> neuron to index }.toMap()
            val neurons2 = addNeuronCollection(neuronCount).neuronList.mapIndexed { index, neuron -> neuron to index }.toMap()
            val syns1 = strategy1.connectNeurons(neurons1.keys.toList(), neurons1.keys.toList())
            val syns2 = strategy2.connectNeurons(neurons2.keys.toList(), neurons2.keys.toList())
            val compliments = syns1.map { neurons1[it.source] to neurons1[it.target] } complement syns2.map { neurons2[it.source] to neurons2[it.target] }
            if (expectIdentical) {
                assert(compliments.isIdentical()) {
                    "Connectivities are not identical: $compliments"
                }
            } else {
                assert(!compliments.isIdentical()) {
                    "Connectivities are identical: $compliments"
                }
            }
        }
    }
}


