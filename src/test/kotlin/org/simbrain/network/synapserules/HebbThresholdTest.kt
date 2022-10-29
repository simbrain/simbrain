package org.simbrain.network.synapserules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.synapse_update_rules.HebbianRule
import org.simbrain.network.synapse_update_rules.HebbianThresholdRule
import org.simbrain.network.synapse_update_rules.OjaRule

class HebbThresholdTest {

    var net = Network()
    val n1 = Neuron(net)
    val n2 = Neuron(net)
    var s12 = Synapse(n1,n2)

    init {
        net.addNetworkModels(n1, n2, s12)
        s12.learningRule = HebbianThresholdRule().apply {
            learningRate = 1.0
            outputThreshold = 0.5
            outputThresholdMomentum = 0.1
            useSlidingOutputThreshold = false
        }

        s12.strength = 0.0
        n1.isClamped = true
        n2.isClamped = true
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