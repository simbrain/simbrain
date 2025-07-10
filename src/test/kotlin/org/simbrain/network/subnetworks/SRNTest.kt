package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SRNTrainer
import org.simbrain.network.trainers.createDiagonalDataset
import org.simbrain.network.trainers.splitDataSet
import smile.math.matrix.Matrix
import kotlin.math.ceil

class SRNTest {

    val net = Network()
    val srn = SRNNetwork(10, 5, 10).apply {
        label = "SRN"
    }

    init {
        net.addNetworkModels(srn)
    }

    @Test
    fun `test basic update`() {
        // Simple task
        srn.trainingSet = createDiagonalDataset(10, 10, shiftAmount = 1)
        srn.testingSet = MatrixDataset(
            inputs = Matrix(ceil(srn.trainingSet.inputs.nrow() * 0.2).toInt(), srn.trainingSet.inputs.ncol()),
            targets = Matrix(ceil(srn.trainingSet.targets.nrow() * 0.2).toInt(), srn.trainingSet.targets.ncol())
        )
        with(net) {
            srn.randomize()
            srn.update()
            srn.trainerConfig.learningRate = 0.01

            val trainer = SRNTrainer(net, srn)

            println(trainer.lastTrainingError)
            runBlocking {
                repeat(1500) {
                    trainer.trainOnce()
                }
            }
            println(trainer.lastTrainingError)
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