package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.SupervisedTrainer.UpdateMethod
import smile.math.matrix.Matrix

class BackpropTest {

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(10,7,10), null).apply {
        label = "backprop"
        trainingSet = MatrixDataset(
            inputs = Matrix.eye(10),
            targets = Matrix.eye(10)
        )
    }

    init {
        net.addNetworkModels(bp)
    }

    @Test
    fun `test backprop learning`() {
        bp.trainer.updateType = UpdateMethod.Epoch()
        with(net) {
            with(bp) {
                runBlocking {
                    repeat(100) {
                        trainer.trainOnce()
                        println(trainer.lastError)
                    }
                }
            }
        }

    }

    @Test
    fun `test backprop network serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        Assertions.assertNotNull(fromXml.getModelByLabel(BackpropNetwork::class.java, "backprop"))
    }

}