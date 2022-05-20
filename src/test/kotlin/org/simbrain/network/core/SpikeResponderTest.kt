package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule
import org.simbrain.network.synapse_update_rules.spikeresponders.Step

class SpikeResponderTest {

    val net = Network()

    @Test
    fun `test spiking threshold`() {
        val rule = SpikingThresholdRule()
        rule.threshold = .7
        val n1 = Neuron(net, rule)
        val n2 = Neuron(net)
        n2.upperBound = 10.0
        val s = Synapse(n1, n2)
        val step = Step()
        step.responseHeight = 2.0
        step.responseDuration = 3.0
        s.spikeResponder = step
        net.timeStep = 1.0 // For simpler computations
        net.addNetworkModels(listOf(n1, n2, s))
        println(s.spikeResponder)

        // Sub-threshold.
        n1.addInputValue(.6)
        net.update()
        net.update()
        assertEquals(0.0, n2.activation)

        // Above-threshold. Should "fire" at responseHeight (2.0) for responseDuration (3)
        n1.addInputValue(.8)
        net.update()
        net.update()  // TODO: Why does it take an extra step?
        assertEquals(2.0, n2.activation)
        net.update()
        assertEquals(2.0, n2.activation)
        net.update()
        assertEquals(2.0, n2.activation)
        net.update()
        assertEquals(0.0, n2.activation)

        // repeat(10) {
        //     if (it == 3) n1.addInputValue(1.0)
        //     net.update()
        //     println("$it:${n1.activation} -> ${n2.activation}")
        // }
    }


}