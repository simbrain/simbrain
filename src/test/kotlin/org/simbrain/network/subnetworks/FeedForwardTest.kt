package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class FeedForwardTest {

    val net = Network()
    val ff = FeedForward(intArrayOf(2,3,4), null).apply {
        label = "ff"
    }

    init {
        net.addNetworkModels(ff)
    }

    @Test
    fun `test FF network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(FeedForward::class.java, "ff"))
    }

}