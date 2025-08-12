package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.createDiagonalDataset

class SRNTest {

    @Test
    fun `test srn training`() {
        val net = Network()
        val srn = SRNNetwork(10, 5, 10).apply {
            label = "SRN"
        }
        net.addNetworkModelsAsync(srn)
        srn.trainingSet = createDiagonalDataset(10, 10, shiftAmount = 1)
        with(net) {
            srn.randomize()
            srn.update()
            srn.trainerConfig.learningRate = 0.01

            val trainer = SupervisedTrainer(net, srn)

            runBlocking {
                repeat(1500) {
                    trainer.trainOnce()
                }
            }
            //println(trainer.lastTrainingError)
            assert(trainer.lastTrainingError < 0.1) { "Error too high: ${trainer.lastTrainingError}" }
        }
    }

    @Test
    fun `test SRN serialization`() {
        val net = Network()
        val srn = SRNNetwork(10, 5, 10).apply {
            label = "SRN"
        }
        net.addNetworkModelsAsync(srn)
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(SRNNetwork::class.java, "SRN"))
    }

}