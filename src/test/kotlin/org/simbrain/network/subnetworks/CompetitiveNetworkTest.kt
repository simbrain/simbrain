package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class CompetitiveNetworkTest {

    val net = Network()
    val cn = CompetitiveNetwork(3, 2).apply {
        label = "Competitive"
    }

    init {
        net.addNetworkModelsAsync(cn)
    }

    @Test
    fun `test competitive network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(CompetitiveNetwork::class.java, "Competitive"))
    }

}