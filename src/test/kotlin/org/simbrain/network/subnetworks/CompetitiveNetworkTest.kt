package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
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
    fun `test competitive network copy`() {
        // Set up original network
        cn.trainer.learningRate = 0.1
        val testInput = doubleArrayOf(0.5, 0.3, 0.8)
        with(net) {
            cn.inputLayer.activationArray = testInput
            cn.trainOnCurrentPattern()
        }

        // Create copy and verify structure
        val copy = cn.copy()
        assertEquals(cn.competitive.size, copy.competitive.size, "Competitive layer size should match")
        assertEquals(cn.inputLayer.size, copy.inputLayer.size, "Input layer size should match")
        assertEquals(cn.modelList.get<SynapseGroup>().size, copy.modelList.get<SynapseGroup>().size,
            "Should have same number of synapse groups")

        // Verify properties
        assertEquals(cn.trainer.learningRate, copy.trainer.learningRate, "Learning rate should be copied")

        // Test behavior
        with(net) {
            // Test original
            cn.inputLayer.activationArray = testInput
            cn.update()
            val originalActivations = cn.competitive.activationArray

            // Test copy
            copy.inputLayer.activationArray = testInput
            copy.update()
            val copyActivations = copy.competitive.activationArray

            // Compare activations
            originalActivations.zip(copyActivations).forEach { (orig, copied) ->
                assertEquals(orig, copied, 0.0001, "Neuron activations should match")
            }
        }
    }

    @Test
    fun `test competitive network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(CompetitiveNetwork::class.java, "Competitive"))
    }

}
