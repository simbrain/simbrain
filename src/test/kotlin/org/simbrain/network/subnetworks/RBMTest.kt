package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class RBMTest {

    val net = Network()
    val srn = RestrictedBoltzmannMachine(3, 2).apply {
        label = "RBM"
    }

    init {
        net.addNetworkModelsAsync(srn)
    }

    @Test
    fun `test RBM serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(RestrictedBoltzmannMachine::class.java, "RBM"))
    }

}