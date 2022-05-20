package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule

class SpikingThresholdTest {

    val net = Network()

    @Test
    fun `test spiking threshold`() {
        val rule = SpikingThresholdRule()
        rule.threshold = .3
        val n = Neuron(net, rule)

        // No spike initially
        assertEquals(0.0, n.activation)
        assertEquals(false, n.isSpike)

        // Add more than threshold and it should spike
        n.addInputValue(.5)
        n.update()
        assertEquals(1.0, n.activation)
        assertEquals(true, n.isSpike)

        // Back to normal
        n.update()
        assertEquals(false, n.isSpike)
        assertEquals(0.0, n.activation)

        // Add less than threshold and it should not spike
        n.addInputValue(.2)
        n.update()
        assertEquals(false, n.isSpike)
        assertEquals(0.0, n.activation)
    }

}