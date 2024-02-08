package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.NeuronNode

class SOMTest {

    val net = Network()

    @Test
    fun `test som`() {
        val som = SOMNetwork(1, 2)
        net.addNetworkModelAsync(som)
        assertEquals(0, net.freeNeurons.size)
        assertEquals(1, som.som.size())
        assertEquals(2, som.inputLayer.size())
        assertEquals(0, net.freeSynapses.size)
        assertEquals(0, som.modelList.get<Synapse>().size)
        assertEquals(2, som.modelList.get<SynapseGroup>().first().size())
    }

    @Test
    fun `test som node creation`() {
        runBlocking {
            val som = SOMNetwork(1, 2)
            val np = NetworkPanel(NetworkComponent("Test", net))
            net.addNetworkModel(som)
            assertEquals(3, np.filterScreenElements<NeuronNode>().size)
        }
    }

}