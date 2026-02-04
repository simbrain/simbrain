package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SynapseTestKt {

    val network = Network()

    @Test
    fun `duplicate synapses shouldn't be added`() {
        val (n1, n2) = List(2) { Neuron().also { network.addNetworkModelAsync(it) } }
        List(2) { Synapse(n1, n2).also { network.addNetworkModelAsync(it) } }
        assertEquals(1, n1.fanOut.size)
        assertEquals(1, n2.fanIn.size)
    }

    @Test
    fun `delay of 0 should not use delay manager`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 0
        source.activation = 1.0
        synapse.forceSetStrength(2.0)

        with(network) {
            synapse.updatePSR()
        }

        assertEquals(2.0, synapse.psr, 0.001)
    }

    @Test
    fun `delay of 1 should delay PSR by one step`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 1
        synapse.forceSetStrength(1.0)
        source.activation = 5.0

        with(network) {
            synapse.updatePSR()
        }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) {
            synapse.updatePSR()
        }
        assertEquals(5.0, synapse.psr, 0.001)
    }

    @Test
    fun `delay of 3 should delay PSR by three steps`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 3
        synapse.forceSetStrength(1.0)
        source.activation = 10.0

        with(network) {
            synapse.updatePSR()
        }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) {
            synapse.updatePSR()
        }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) {
            synapse.updatePSR()
        }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) {
            synapse.updatePSR()
        }
        assertEquals(10.0, synapse.psr, 0.001)
    }

    @Test
    fun `delay manager should handle multiple different values`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 3
        synapse.forceSetStrength(1.0)

        source.activation = 1.0
        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        source.activation = 2.0
        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        source.activation = 3.0
        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        source.activation = 4.0
        with(network) { synapse.updatePSR() }
        assertEquals(1.0, synapse.psr, 0.001)

        source.activation = 5.0
        with(network) { synapse.updatePSR() }
        assertEquals(2.0, synapse.psr, 0.001)

        source.activation = 6.0
        with(network) { synapse.updatePSR() }
        assertEquals(3.0, synapse.psr, 0.001)
    }

    @Test
    fun `delay manager circular buffer should wrap around correctly`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 2
        synapse.forceSetStrength(1.0)

        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0)
        val expectedOutputs = listOf(0.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

        values.zip(expectedOutputs).forEach { (input, expected) ->
            source.activation = input
            with(network) { synapse.updatePSR() }
            assertEquals(expected, synapse.psr, 0.001)
        }
    }

    @Test
    fun `changing delay should reset delay manager`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 2
        synapse.forceSetStrength(1.0)
        source.activation = 5.0

        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        synapse.delay = 1

        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) { synapse.updatePSR() }
        assertEquals(5.0, synapse.psr, 0.001)
    }

    @Test
    fun `setting delay to 0 should remove delay manager`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 3
        synapse.forceSetStrength(1.0)
        source.activation = 5.0

        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        synapse.delay = 0

        with(network) { synapse.updatePSR() }
        assertEquals(5.0, synapse.psr, 0.001)
    }

    @Test
    fun `negative delay should be rejected`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 2
        val initialDelay = synapse.delay

        synapse.delay = -1

        assertEquals(initialDelay, synapse.delay)
    }

    @Test
    fun `delay should work with non-unit synapse strength`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 2
        synapse.forceSetStrength(3.0)
        source.activation = 4.0

        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)

        with(network) { synapse.updatePSR() }
        assertEquals(12.0, synapse.psr, 0.001)
    }

    @Test
    fun `clear should reset delay manager queue`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 2
        synapse.forceSetStrength(1.0)
        source.activation = 5.0

        with(network) { synapse.updatePSR() }
        with(network) { synapse.updatePSR() }

        synapse.clear()

        with(network) { synapse.updatePSR() }
        assertEquals(0.0, synapse.psr, 0.001)
    }

    @Test
    fun `disabled synapse should not update PSR with delay`() {
        val network = Network()
        val source = Neuron()
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)
        val synapse = Synapse(source, target)
        network.addNetworkModelAsync(synapse)

        synapse.delay = 1
        synapse.forceSetStrength(1.0)
        synapse.isEnabled = false
        source.activation = 5.0

        with(network) { synapse.updatePSR() }

        assertEquals(0.0, synapse.psr, 0.001)
    }

}