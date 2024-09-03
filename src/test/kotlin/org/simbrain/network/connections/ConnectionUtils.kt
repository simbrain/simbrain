package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import kotlin.random.Random

class ConnectionUtils {

    @Test
    fun `check that polarizeSynapses produces the appropriate percentage of excitatory synapses`() {
        val net = Network()
        val sourceNeuron = Neuron()
        val syns = List(100) {
            Synapse(sourceNeuron, Neuron())
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

    @Test
    fun `polarizeSynapses should set strength on all synapses`() {
        val net = Network()
        val sourceNeuron = Neuron()
        val syns = List(101) {
            Synapse(sourceNeuron, Neuron()).apply { strength = 0.1 }
        }
        polarizeSynapses(syns , 50.0)
        assertTrue(syns.all { it.strength == 1.0 || it.strength == -1.0 }) {
            syns.mapIndexed { index, synapse -> index to synapse.strength }
                .filter { (_, strength) -> strength != 1.0 && strength != -1.0 }
                .map { (index, _) -> index }
                .joinToString(", ")
                .let { "Synapse indices with unexpected strength: [$it]" }
        }
    }

    @Test
    fun `polarizeSynapses should produce the same pattern when given the same seed`() {
        val net = Network()
        val sourceNeuron = Neuron()
        val syns = List(100) {
            Synapse(sourceNeuron, Neuron())
        }
        polarizeSynapses(syns , 50.0, Random(1234))
        val syns1 = syns.map { it.strength }
        polarizeSynapses(syns , 50.0, Random(1234))
        val syns2 = syns.map { it.strength }
        assertArrayEquals(syns1.toDoubleArray(), syns2.toDoubleArray())
    }



}