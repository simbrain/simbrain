package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class HopfieldTest {

    val net = Network()
    val hopfield = Hopfield(10).apply {
        label = "Hopfield"
    }

    init {
        net.addNetworkModelsAsync(hopfield)
    }

    @Test
    fun `test hopfield network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(Hopfield::class.java, "Hopfield"))
    }

}