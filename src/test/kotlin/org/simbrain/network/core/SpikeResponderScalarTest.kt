package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule
import org.simbrain.network.spikeresponders.StepResponder

class SpikeResponderScalarTest {

    val net = Network()
    val n1 = Neuron(net) // Input
    val n2 = Neuron(net, SpikingThresholdRule()) // Spiking neuron
    val n3 = Neuron(net) // receive spike response
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModels(n1, n2, n3, s1, s2)
    }

    @Test
    fun `responder data is copied correctly`() {
        val step = StepResponder()
        val newResponder: StepResponder = step.copy() as StepResponder
        assertEquals(step.responseDuration, newResponder.responseDuration)
        assertEquals(step.responseHeight, newResponder.responseHeight)
    }

    /**
     * Should "fire" at responseHeight (2.0) for responseDuration (3)
     */
    @Test
    fun `step responder produces correct height and duration `() {

        val step = StepResponder()
        step.responseHeight = .75
        step.responseDuration = 3
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update() // First update propagates from n1 to n2, no spike response yet
        assertEquals(0.0, s2.psr)
        assertEquals(0.0, n3.activation)
        net.update()
        assertEquals(step.responseHeight, s2.psr)
        assertEquals(step.responseHeight, n3.activation)
        net.update()
        assertEquals(step.responseHeight, s2.psr)
        assertEquals(step.responseHeight, n3.activation)
        net.update()
        assertEquals(step.responseHeight, s2.psr)
        assertEquals(step.responseHeight, n3.activation)
        net.update()
        assertEquals(0.0, s2.psr)
        assertEquals(0.0, n3.activation)
    }

}