package org.simbrain.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.getModelById
import org.simbrain.network.core.getNetworkXStream

class NetworkKtTests {

    val net = Network()
    val n1 = Neuron().apply {
        clamped = true
        activation = 1.0
    }
    val n2 = Neuron()

    init {
        net.addNetworkModelsAsync(n1, n2)
    }

    @Test
    fun `test deserialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        val n1 = fromXml.getModelById<Neuron>("Neuron_1")
        assertEquals(1.0, n1.activation)
    }


}