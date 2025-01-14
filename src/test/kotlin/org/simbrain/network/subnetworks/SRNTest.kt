package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.SRNTrainer

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
            srn.trainerConfig.learningRate = 0.01

            val trainer = SRNTrainer(net, srn)

            runBlocking {
                repeat(1000) {
                    trainer.trainOnce()
                }
            }
            // print(srn.trainer.lastError)
            assert(trainer.lastTrainingError < 0.1) { "Error too high: ${trainer.lastTrainingError}" }
        }
    }

    @Test
    fun `test SRN serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(SRNNetwork::class.java, "SRN"))
    }

}