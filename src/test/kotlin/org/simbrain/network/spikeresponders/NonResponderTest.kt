package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.SpikingThresholdRule

class NonResponderTest {

    val net = Network()
    val n1 = Neuron() // Input
    val n2 = Neuron(SpikingThresholdRule()) // Spiking neuron
    val n3 = Neuron().also { it.upperBound = 10.0 } // receive spike response
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModels(n1, n2, n3, s1, s2)
    }

    @Test
    fun `basic non responder passes weight through`() {
        val nonResponder = NonResponder()
        s2.spikeResponder = nonResponder
        s2.strength = 0.7
        n1.activation = 1.0
        net.update()
        net.update()
        // Just passes the weight through
        assertEquals(0.7, n3.activation)
    }

    @Test
    fun `non responder with negative weight`() {
        val nonResponder = NonResponder()
        s2.spikeResponder = nonResponder
        s2.strength = -0.5
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(-0.5, n3.activation)
    }

    @Test
    fun `non responder with zero weight`() {
        val nonResponder = NonResponder()
        s2.spikeResponder = nonResponder
        s2.strength = 0.0
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `non responder copy preserves properties`() {
        val original = NonResponder()
        val copy = original.copy()
        assertEquals(original.description, copy.description)
        assertEquals(original.name, copy.name)
    }

    @Test
    fun `non responder description and name`() {
        val nonResponder = NonResponder()
        assertEquals("None (No spike response)", nonResponder.description)
        assertEquals("None", nonResponder.name)
    }

    @Test
    fun `non responder with multiple spikes`() {
        val nonResponder = NonResponder()
        s2.spikeResponder = nonResponder
        s2.strength = 0.3
        
        // First spike
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.3, n3.activation)
        
        // Second spike
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.3, n3.activation)
    }

    @Test
    fun `non responder with varying source activation`() {
        val nonResponder = NonResponder()
        s2.spikeResponder = nonResponder
        s2.strength = 0.4
        
        // High activation
        n1.activation = 2.0
        net.update()
        net.update()
        assertEquals(0.4, n3.activation)
        
        // Low activation
        n1.activation = 0.5
        net.update()
        net.update()
        assertEquals(0.4, n3.activation)
    }
} 