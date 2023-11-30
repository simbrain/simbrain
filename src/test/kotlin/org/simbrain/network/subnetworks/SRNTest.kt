package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network

class SRNTest {

    val net = Network()
    val srn = SRNNetwork(net, 3, 5, 3)

    init {
        net.addNetworkModelsAsync(srn)
    }

    @Test
    fun `test`() {
        srn.randomize()
        srn.update()
        print(srn.outputLayer.activations)
        srn.trainer.learningRate = 0.01
        runBlocking {
            srn.trainer.train(5000)
        }
        srn.inputLayer.setActivations(DoubleArray(3) { 0.0 }.also { it[0] = 1.0 })
        srn.update()
        // Expecting 0,1,0
        // Assertions.assertArrayEquals()
        print(srn.outputLayer.activations)
    }

}