package org.simbrain.network.synapserules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.synapse_update_rules.OjaRule

class OjaTest {

    // 2->1 network
    var net = Network()
    val input = Neuron(net)
    val output = Neuron(net)
    var weight = Synapse(input,output)

    init {
        net.addNetworkModelsAsync(input, output, weight)
        weight.learningRule = OjaRule().apply {
            learningRate = 1.0
            normalizationFactor = 1.0
        }
        weight.strength = 0.0
        weight.upperBound = 10.0
        weight.lowerBound = -10.0
        input.isClamped = true
        output.isClamped = true
    }

    @Test
    fun `test update for a single weight and clamped nodes`() {
        input.forceSetActivation(1.0)
        output.forceSetActivation(1.0)
        net.update()

        // delta-w  = rate (out(in - out * weight))
        //          = 1 (1(1 - 1*0)) = 1
        assertEquals(1.0, weight.strength)
        net.update()
        //          1 (1(1 - 1*1)) = 0
        assertEquals(1.0, weight.strength)
        repeat(10) {
            net.update()
        }
        assertEquals(1.0, weight.strength)
    }

    @Test
    fun `test update with different source and target`() {
        // High learning rate leads to divergence in this case
        input.forceSetActivation(1.0)
        output.forceSetActivation(2.0)
        // delta-w  = rate (out(in - out * weight))
        //          = 1 (2(1 - 2*0)) = 2
        // Weight becomes 0 + 2 = 2
        net.update()
        assertEquals(2.0, weight.strength)
        //          = 1 (2(1 - 2*2)) = -6
        // Weight becomes 2 -6 = -4
        net.update()
        assertEquals(-4.0, weight.strength)
        //          = 1 (2(1 - 2*-4)) = 18
        // Weight becomes -6 + 18 = 12, which is clipped at 10
        net.update()
        assertEquals(10.0, weight.strength)
    }

    @Test
    fun `test update with learning rate less than 1`() {
        input.forceSetActivation(1.0)
        output.forceSetActivation(1.0)
        (weight.learningRule as OjaRule).learningRate = .1

        // Should approach 1.
        repeat(100) {
            net.update()
        }
        assertEquals(1.0, weight.strength, .01)

        // Should approach -1
        output.forceSetActivation(-1.0)
        repeat(100) {
            net.update()
        }
        assertEquals(-1.0, weight.strength, .01)

        // Should approach 1/2
        output.forceSetActivation(2.0)
        repeat(100) {
            net.update()
        }
        assertEquals(.5, weight.strength, .01)

        // Should approach 1/3
        output.forceSetActivation(3.0)
        repeat(100) {
            net.update()
        }
        assertEquals(.33, weight.strength, .01)

        // Should approach -1/3
        output.forceSetActivation(-3.0)
        repeat(100) {
            net.update()
        }
        assertEquals(-.33, weight.strength, .01)

    }

}