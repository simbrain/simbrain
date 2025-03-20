package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class RestrictedBoltzmannMachineTest {

    val net = Network()
    val rbm = RestrictedBoltzmannMachine(4, 3).apply {
        label = "RBM"
    }

    init {
        net.addNetworkModels(rbm)
    }

    @Test
    fun `test rbm network copy`() {
        // Set up original network
        rbm.trainer.learningRate = 0.1
        val testInput = doubleArrayOf(0.5, 0.3, 0.8, 0.2)
        with(net) {
            rbm.visibleLayer.activationArray = testInput
            rbm.trainOnCurrentPattern()
        }

        // Create copy and verify structure
        val copy = rbm.copy()
        assertEquals(rbm.hiddenLayer.size, copy.hiddenLayer.size, "Hidden layer size should match")
        assertEquals(rbm.visibleLayer.size, copy.visibleLayer.size, "Visible layer size should match")
        assertEquals(rbm.modelList.get<SynapseGroup>().size, copy.modelList.get<SynapseGroup>().size,
            "Should have same number of synapse groups")
            
        // Verify properties
        assertEquals(rbm.trainer.learningRate, copy.trainer.learningRate, "Learning rate should be copied")
    }

    @Test
    fun `test rbm network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(RestrictedBoltzmannMachine::class.java, "RBM"))
    }
}