package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class NetworkPanelTest {
    @Test
    fun testAddingScreenElements() {
        runBlocking {
            val net = Network()
            val nc = NetworkComponent("Test", net)
            val np = NetworkPanel(nc)
            val n1 = Neuron(net)
            val n2 = Neuron(net)
            net.addNetworkModel(n1)
            net.addNetworkModel(n2)?.join()
            assertEquals(2, np.screenElements.size)
        }
    }
}