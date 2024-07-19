package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.NeuronNode

class SOMTest {

    val net = Network()
    val som = SOMNetwork(2, 1).apply {
        label = "SOM"
    }

    init {
        net.addNetworkModel(som)
    }

    @Test
    fun `test som`() {
        assertEquals(0, net.freeNeurons.size)
        assertEquals(1, som.som.size)
        assertEquals(2, som.inputLayer.size)
        assertEquals(0, net.freeSynapses.size)
        assertEquals(0, som.modelList.get<Synapse>().size)
        assertEquals(2, som.modelList.get<SynapseGroup>().first().size())
    }

    @Test
    fun `test som node creation`() {
        runBlocking {
            val np = NetworkPanel(NetworkComponent("Test", net))
            assertEquals(3, np.filterScreenElements<NeuronNode>().size)
        }
    }

    @Test
    fun `test SOM serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(SOMNetwork::class.java, "SOM"))
    }

}