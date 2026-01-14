package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

class ConstantWeightInitializerTest {

    @Test
    fun `should initialize excitatory synapses with specified strength`() {
        val initializer = ConstantWeightInitializer().apply {
            excitatoryStrength = 2.5
        }
        val synapses = List(10) { Synapse(Neuron(), Neuron()) }
        val polarized = PolarizedSynapseCollection(excitatory = synapses, inhibitory = emptyList())

        initializer.initializeWeights(polarized)

        assertTrue(synapses.all { it.strength == 2.5 })
    }

    @Test
    fun `should initialize inhibitory synapses with specified strength`() {
        val initializer = ConstantWeightInitializer().apply {
            inhibitoryStrength = -3.0
        }
        val synapses = List(10) { Synapse(Neuron(), Neuron()) }
        val polarized = PolarizedSynapseCollection(excitatory = emptyList(), inhibitory = synapses)

        initializer.initializeWeights(polarized)

        assertTrue(synapses.all { it.strength == -3.0 })
    }

    @Test
    fun `should initialize both excitatory and inhibitory synapses correctly`() {
        val initializer = ConstantWeightInitializer().apply {
            excitatoryStrength = 1.5
            inhibitoryStrength = -2.0
        }
        val excitatorySynapses = List(5) { Synapse(Neuron(), Neuron()) }
        val inhibitorySynapses = List(5) { Synapse(Neuron(), Neuron()) }
        val polarized = PolarizedSynapseCollection(excitatory = excitatorySynapses, inhibitory = inhibitorySynapses)

        initializer.initializeWeights(polarized)

        assertTrue(excitatorySynapses.all { it.strength == 1.5 })
        assertTrue(inhibitorySynapses.all { it.strength == -2.0 })
    }

    @Test
    fun `copy should create independent copy with same values`() {
        val original = ConstantWeightInitializer().apply {
            excitatoryStrength = 5.0
            inhibitoryStrength = -4.0
        }

        val copy = original.copy()

        assertEquals(original.excitatoryStrength, copy.excitatoryStrength)
        assertEquals(original.inhibitoryStrength, copy.inhibitoryStrength)

        copy.excitatoryStrength = 10.0
        assertEquals(5.0, original.excitatoryStrength)
    }

}
