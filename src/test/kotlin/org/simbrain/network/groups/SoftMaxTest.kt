package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.connect
import org.simbrain.network.neurongroups.SoftmaxGroup

class SoftMaxTest {

    var net = Network()
    val softmax = SoftmaxGroup(2)
    val n1 = Neuron()
    val n2 = Neuron()

    init {
        with(net) {
            net.addNetworkModels(softmax, n1, n2)
            n1.clamped = true
            n2.clamped = true
            connect(n1, softmax.getNeuron(0), 1.0)
            connect(n2, softmax.getNeuron(1), 1.0)
        }
    }

    @Test
    fun `Softmax activations sum to 1`() {
        net.update()
        assertEquals(1.0, softmax.activations.sum(), 0.01)
    }

    @Test
    fun `Equal inputs should produce equal outputs`() {
        n1.activation = 0.85
        n2.activation = 0.85
        net.update()
        assertEquals(softmax.activationArray[0], softmax.activationArray[1])
    }

    @Test
    fun `The node receiving the most input should have the highest value`() {
        n1.activation = 1.0
        n2.activation = 0.5
        net.update()
        assertTrue(softmax.activationArray[0] > softmax.activationArray[1])
    }

    @Test
    fun `Test copy function`() {
        val softmax2 = softmax.copy()
        net.addNetworkModels(softmax2)
        assertEquals(2, softmax2.neuronList.size)
    }
}


