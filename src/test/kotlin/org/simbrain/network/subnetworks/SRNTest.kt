package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.createDiagonalDataset
import org.simbrain.util.stats.distributions.NormalDistribution

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
            // The input layer starts with random activations that reach the context layer through this first update,
            // so it needs the seed too. Some starting points settle in a local minimum.
            val randomizer = NormalDistribution(0.0, .1).apply { randomSeed = 1 }
            srn.randomize(randomizer)
            srn.inputLayer.randomize(randomizer)
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