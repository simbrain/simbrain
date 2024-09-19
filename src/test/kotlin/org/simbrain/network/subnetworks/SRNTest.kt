package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class SRNTest {

    val net = Network()
    val srn = SRNNetwork(10, 5, 10).apply {
        label = "SRN"
    }

    init {
        net.addNetworkModels(srn)
    }

    @Test
    fun `test`() {
        with(net) {
            srn.randomize()
            srn.update()
            srn.trainer.learningRate = 0.01
            runBlocking {
                srn.trainer.run { srn.train(10000) }
            }
            // print(srn.trainer.lastError)
            assert(srn.trainer.lastError < 0.1) { "Error too high: ${srn.trainer.lastError}" }
        }
    }

    @Test
    fun `test SRN serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(SRNNetwork::class.java, "SRN"))
    }

}