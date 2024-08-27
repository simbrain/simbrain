package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.SimbrainConstants

class NeuronTest {

    var net = Network()
    val n1 = Neuron();
    val n2 = Neuron();
    val n3 = Neuron();
    var s1: Synapse
    var s2: Synapse

    init {
        net.addNetworkModels(listOf(n1, n2, n3))
        s1 = net.addSynapse(n1, n3)
        s2 = net.addSynapse(n2, n3)
    }

    @Test
    fun `test propagation in a 2-1 network`() {
        n1.addInputValue(.1)
        n2.addInputValue(.1)
        n1.activation = .2
        n2.activation = .5
        net.update()
        assertEquals(.7, n3.activation) // Just gets source activation
        net.update()
        assertEquals(.2, n3.activation) // Inputs have no made it up
    }

    @Test
    fun `test polarity change`() {
        s1.strength = 1.5
        n1.polarity = SimbrainConstants.Polarity.INHIBITORY
        assertTrue { s1.strength  == -1.5  }
    }

    @Test
    fun `test excitatory inputs`() {
        s1.strength = 1.0
        s1.psr = .5
        s2.strength = 1.0
        s2.psr = 1.0
        assertEquals(1.5, n3.excitatoryInputs)
        assertEquals(0.0, n3.inhibitoryInputs)
        // Excitatory input can in principle be negative, when the synapse is excitatory, but the psr is negative
        s2.psr = -1.0
        assertEquals(-.5, n3.excitatoryInputs)
    }

    @Test
    fun `test inhibitory inputs`() {
        s1.strength = -1.0
        s1.psr = .5
        s2.strength = -1.0
        s2.psr = 1.0
        assertEquals(1.5, n3.inhibitoryInputs)
        assertEquals(0.0, n3.excitatoryInputs)
    }

}