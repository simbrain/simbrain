package org.simbrain.network.learningrules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

class SubtractiveNormalizationTest {

    var net = Network()
    val n1 = Neuron()
    val n2 = Neuron()
    var s12 = Synapse(n1,n2)

    init {
        net.addNetworkModelsAsync(n1, n2, s12)
        s12.learningRule = SubtractiveNormalizationRule().apply {
            learningRate = 1.0
        }
        s12.strength = 0.0
        n1.clamped = true
        n2.clamped = true
    }

    @Test
    fun `test basic update`() {
        n1.forceSetActivation(1.0)
        n2.forceSetActivation(1.0)
        net.update()
//        assertEquals(1.0,s12.strength )
//        net.update()
//        assertEquals(2.0,s12.strength )
        println("Strength is ${s12.strength}")
    }
}