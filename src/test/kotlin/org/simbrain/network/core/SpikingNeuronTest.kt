package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.SpikingThresholdRule

class SpikingNeuronTest {

    var net = Network()
    val n1 = Neuron();
    val n2 = Neuron();
    val n3 = Neuron();

    init {
        net.addNetworkModels(listOf(n1, n2, n3))
        net.addSynapse(n1, n2)
        net.addSynapse(n2, n3)
    }


    @Test
    fun `test a spike occurs and stops`() {
        n1.activation = 1.0
        val rule = SpikingThresholdRule()
        rule.threshold = .5
        n2.updateRule = rule
        with(net) {
            update()
            assertTrue(n2.isSpike)
            update()
            assertFalse(n2.isSpike)
        }
    }

    @Test
    fun `test last spike time is accurate`() {
        net.update() // advance time past 0
        n1.activation = 1.0
        val rule = SpikingThresholdRule()
        rule.threshold = .5
        n2.updateRule = rule
        val spikeTime = net.time
        net.update()
        net.update()
        assertEquals(spikeTime, n2.lastSpikeTime)
    }


}