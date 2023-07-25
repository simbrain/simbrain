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
        srn.update()
        print(srn.outputLayer.activations)
        runBlocking {
            repeat(1000) {
                srn.trainer.iterate()
            }
        }
        srn.inputLayer.setActivations(doubleArrayOf(1.0, 0.0, 0.0))
        srn.update()
        // Expecting 0,1,0
        print(srn.outputLayer.activations)
    }

}