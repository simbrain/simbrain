package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NetworkUtilsTest {

    val net = Network()

    @Test
    fun `test energy function`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        val weight = Synapse(neuron1, neuron2)
        neuron1.activation = 1.0
        neuron2.activation = 1.0
        weight.strength = 1.0
        // Energy is -.5 * neuron1 activation * neuron 2 activaton * weight strength
        assertEquals(-.5, listOf(neuron1, neuron2).getEnergy())

        neuron1.activation = -1.0
        assertEquals(.5, listOf(neuron1, neuron2).getEnergy(), .01)

        neuron2.activation = 0.0
        assertEquals(0.0, listOf(neuron1, neuron2).getEnergy(), .01)
    }

    @Test
    fun `test clamp neurons`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        listOf(neuron1, neuron2).clamp(true)
        assertEquals(true, neuron1.clamped)
        assertEquals(true, neuron2.clamped)
        listOf(neuron1, neuron2).clamp(false)
        assertEquals(false, neuron1.clamped)
        assertEquals(false, neuron2.clamped)
    }

}