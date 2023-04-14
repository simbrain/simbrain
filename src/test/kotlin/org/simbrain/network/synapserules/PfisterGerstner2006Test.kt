package org.simbrain.network.synapserules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.synapse_update_rules.PfisterGerstner2006Rule

class PfisterGerstner2006Test {

    var net = Network()
    val n1 = Neuron(net)
    val n2 = Neuron(net)
    var s12 = Synapse(n1,n2)

    init {
        net.addNetworkModelsAsync(n1, n2, s12)
        s12.learningRule = PfisterGerstner2006Rule().apply {

        }
        s12.strength = 0.0
        n1.isClamped = true
        n2.isClamped = true
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