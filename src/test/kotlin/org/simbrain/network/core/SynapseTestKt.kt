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

}