package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

class ConnectionUtils {

    @Test
    fun `check that polarizeSynapses produces the appropriate percentage of excitatory synapses`() {
        val net = Network()
        val sourceNeuron = Neuron(net)
        val syns = List(100) {
            Synapse(sourceNeuron, Neuron(net))
        }
        polarizeSynapses(syns , 50.0)
        var numExcitatory = syns.count { s -> s.strength > 0.0 }
        assertEquals(50, numExcitatory)
        polarizeSynapses(syns , 35.0)
        numExcitatory = syns.count { s -> s.strength > 0.0 }
        assertEquals(35, numExcitatory)
        polarizeSynapses(syns , 0.0)
        numExcitatory = syns.count { s -> s.strength > 0.0 }
        assertEquals(0, numExcitatory)
        polarizeSynapses(syns , 100.0)
        numExcitatory = syns.count { s -> s.strength > 0.0 }
        assertEquals(100, numExcitatory)
    }



}