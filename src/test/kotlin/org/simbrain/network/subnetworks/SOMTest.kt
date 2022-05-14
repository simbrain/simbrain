package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.gui.NetworkPanel

class SOMTest {

    val net = Network()

    @Test
    fun `test som`() {
        val som = SOMNetwork(net, 1, 2)
        net.addNetworkModel(som)
        assertEquals(0, net.looseNeurons.size)
        assertEquals(1, som.som.size())
        assertEquals(2, som.inputLayer.size())
        assertEquals(0, net.looseWeights.size)
        assertEquals(0, som.modelList.get<Synapse>().size)
        assertEquals(2, som.modelList.get<SynapseGroup2>().first().size())
    }

    @Test
    fun `test som node creation`() {
        val som = SOMNetwork(net, 1, 2)
        val np = NetworkPanel(NetworkComponent("Test", net))
        net.addNetworkModel(som)
        assertEquals(3, np.neuronNodeMapping.keys.size)
    }

}