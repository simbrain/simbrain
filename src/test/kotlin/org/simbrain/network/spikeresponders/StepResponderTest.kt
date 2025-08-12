package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.SpikingThresholdRule

class StepResponderTest {

    val net = Network()
    val n1 = Neuron() // Input
    val n2 = Neuron(SpikingThresholdRule()) // Spiking neuron
    val n3 = Neuron().also { it.upperBound = 10.0 } // receive spike response
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModelsAsync(n1, n2, n3, s1, s2)
    }

    @Test
    fun `step responder produces correct height and duration`() {
        val step = StepResponder()
        s2.strength = 0.75
        step.responseDuration = 3
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update() // First update propagates from n1 to n2, no spike response yet
        assertEquals(0.0, s2.psr)
        assertEquals(0.0, n3.activation)
        net.update()
        assertEquals(s2.strength, s2.psr)
        assertEquals(s2.strength, n3.activation)
        net.update()
        assertEquals(s2.strength, s2.psr)
        assertEquals(s2.strength, n3.activation)
        net.update()
        assertEquals(s2.strength, s2.psr)
        assertEquals(s2.strength, n3.activation)
        net.update()
        assertEquals(0.0, s2.psr)
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `step responder with single duration`() {
        val step = StepResponder()
        s2.strength = 0.5
        step.responseDuration = 1
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update()
        assertEquals(0.0, s2.psr)
        net.update()
        assertEquals(0.5, s2.psr)
        net.update()
        assertEquals(0.0, s2.psr)
    }

    @Test
    fun `step responder with zero duration`() {
        val step = StepResponder()
        s2.strength = 0.8
        step.responseDuration = 0
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.0, s2.psr)
    }

    @Test
    fun `step responder with negative weight`() {
        val step = StepResponder()
        s2.strength = -0.6
        step.responseDuration = 2
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(-0.6, s2.psr)
        net.update()
        assertEquals(-0.6, s2.psr)
        net.update()
        assertEquals(0.0, s2.psr)
    }

    @Test
    fun `step responder copy preserves properties`() {
        val original = StepResponder()
        original.responseDuration = 5
        original.spikeProbability = 0.8
        
        val copy = original.copy()
        assertEquals(original.responseDuration, copy.responseDuration)
        assertEquals(original.spikeProbability, copy.spikeProbability)
    }

    @Test
    fun `step responder description and name`() {
        val step = StepResponder()
        assertEquals("Step", step.description)
        assertEquals("Step", step.name)
    }

    @Test
    fun `step responder with probability always fires`() {
        val step = StepResponder()
        step.responseDuration = 1
        step.spikeProbability = 1.0
        s2.spikeResponder = step
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.5, n3.activation)
    }

    @Test
    fun `step responder with probability never fires`() {
        val step = StepResponder()
        step.responseDuration = 1
        step.spikeProbability = 0.0
        s2.spikeResponder = step
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `step responder with multiple consecutive spikes`() {
        val step = StepResponder()
        s2.strength = 0.4
        step.responseDuration = 2
        s2.spikeResponder = step

        // First spike
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.4, s2.psr)
        
        // Second spike while still in response period
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.4, s2.psr) // Should still be responding
    }

    @Test
    fun `step responder with long duration`() {
        val step = StepResponder()
        s2.strength = 0.3
        step.responseDuration = 10
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.3, s2.psr)
        
        // Check that it's still active after several updates
        repeat(5) { net.update() }
        assertEquals(0.3, s2.psr)
        
        // Check that it eventually stops
        repeat(10) { net.update() }
        assertEquals(0.0, s2.psr)
    }
} 