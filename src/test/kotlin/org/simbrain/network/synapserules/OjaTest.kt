package org.simbrain.network.synapserules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.synapse_update_rules.OjaRule

class OjaTest {

    var net = Network()
    val n1 = Neuron(net)
    val n2 = Neuron(net)
    val out = Neuron(net)
    var s1 = Synapse(n1,out)
    var s2 = Synapse(n2,out)

    init {
        net.addNetworkModels(n1, n2, out, s1, s2)
        s1.learningRule = OjaRule().apply {
            learningRate = 0.2
            normalizationFactor = 1.0
        }
        s1.strength = 0.0
        s2.learningRule = OjaRule().apply {
            learningRate = 0.2
            normalizationFactor = 1.0
        }
        s2.strength = 0.0

        n1.isClamped = true
        n2.isClamped = true
        out.isClamped = true
    }



    @Test
    fun `test basic update`() {
        n1.forceSetActivation(1.0)
        n2.forceSetActivation(1.0)
        out.forceSetActivation(1.0)
        repeat(1000) {net.update()}
//        assertEquals(1.0,s12.strength )
//        net.update()
//        assertEquals(2.0,s12.strength )
        println("Strength is ${s1.strength}")
        println("Strength is ${s2.strength}")
        println("Sum of squares is ${s1.strength * s1.strength + s2.strength * s2.strength}")
    }
}