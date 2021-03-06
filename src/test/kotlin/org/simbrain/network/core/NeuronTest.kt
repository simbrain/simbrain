package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NeuronTest {

    var net = Network()

    @Test
    fun `test propagation in a 2-1 network`() {
        val n1 = Neuron(net);
        val n2 = Neuron(net);
        val n3 = Neuron(net);
        net.addNetworkModels(listOf(n1,n2,n3))
        net.addSynapse(n1, n3)
        net.addSynapse(n2, n3)
        n1.addInputValue(.1)
        n2.addInputValue(.1)
        n1.setActivation(.2)
        n2.setActivation(.5)
        net.update()
        assertEquals(.7, n3.activation) // Just gets source activation
        net.update()
        assertEquals(.2, n3.activation) // Inputs have no made it up
    }

}