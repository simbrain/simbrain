package org.simbrain.network.learningrules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

class HebbThresholdTest {

    var net = Network()
    val n1 = Neuron()
    val n2 = Neuron()
    var s12 = Synapse(n1,n2)

    init {
        net.addNetworkModelsAsync(n1, n2, s12)
        s12.learningRule = HebbianThresholdRule().apply {
            learningRate = 1.0
            outputThreshold = 0.5
            outputThresholdMomentum = 0.1
            useSlidingOutputThreshold = false
        }

        s12.strength = 0.0
        n1.clamped = true
        n2.clamped = true
    }

    @Test
    fun `test basic update`() {
        n1.forceSetActivation(0.5)
        n2.forceSetActivation(0.0)
        net.update()
        //assertEquals(-0.5,s12.strength )
        println("Strength is ${s12.strength}")
    }
}