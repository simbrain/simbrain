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
    val sm = SoftmaxGroup(2)
    val n1 = Neuron()
    val n2 = Neuron()

    init {
        with(net) {
            net.addNetworkModelsAsync(sm, n1, n2)
            n1.clamped = true
            n2.clamped = true
            connect(n1, sm.getNeuron(0), 1.0)
            connect(n2, sm.getNeuron(1), 1.0)
        }
    }

    @Test
    fun `Sum of SoftMaxGroup is equal to 1`(){
        with(net) {
                net.update()
        }
        assertEquals(1.0, sm.activations.sum(), 0.01)
    }

    @Test
    fun `Neuron1 in sm is equal to Neuron2 in sm without activation from outside neurons`() {
        with(net) {
            n1.forceSetActivation(0.0)
            n2.forceSetActivation(0.0)
            net.update()
        }
        assertEquals(sm.activations.get(0), sm.activations.get(1))
        assertEquals(1.0, sm.activations.sum(), 0.01)
    }

    @Test
    fun `Neuron1 in sm is greater than Neuron2 in sm with activation from outside neurons`() {
        with(net) {
            n1.forceSetActivation(1.0)
            n2.forceSetActivation(0.5)
            repeat(5) {
                net.update()
            }
        }
        assertTrue(sm.activations.get(0) > sm.activations.get(1))
        assertEquals(1.0, sm.activations.sum(), 0.01)
    }
}


