package org.simbrain.network.core

import org.junit.Test

class NeuronTest {

    var net = Network()

    @Test
    fun testWeightedInput() {
        val n1 = Neuron(net, "LinearRule")
        val n2 = Neuron(net, "LinearRule")
        val n3 = Neuron(net, "LinearRule")
        net.addObjects(listOf(n1,n2,n3))
        net.addSynapse(n1, n3)
        net.addSynapse(n2, n3)
        n1.setActivation(.2)
        n2.setActivation(.5)
        net.update()
        assert (n3.activation == .7);
    }
}