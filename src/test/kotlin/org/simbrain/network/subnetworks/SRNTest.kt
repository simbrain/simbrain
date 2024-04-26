package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class SRNTest {

    val net = Network()
    val srn = SRNNetwork(3, 5, 3).apply {
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
            print(srn.outputLayer.activations)
            srn.trainer.learningRate = 0.01
            runBlocking {
                srn.trainer.run { srn.train(5000) }
            }
            srn.inputLayer.setActivations(DoubleArray(3) { 0.0 }.also { it[0] = 1.0 })
            srn.update()
            // Expecting 0,1,0
            // Assertions.assertArrayEquals()
            print(srn.outputLayer.activations)
        }
    }

    @Test
    fun `test SRN serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(SRNNetwork::class.java, "SRN"))
    }

}