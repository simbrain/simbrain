package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class BackpropTest {

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(2,3,4), null).apply {
        label = "backprop"
    }

    init {
        net.addNetworkModelsAsync(bp)
    }

    @Test
    fun `test backprop network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(BackpropNetwork::class.java, "backprop"))
    }

}