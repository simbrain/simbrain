package org.simbrain.network.updaterules

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse

class IACRuleTest {

    private fun iacRule() = IACRule().apply { upperBound = 1.0; lowerBound = -1.0; decay = 0.0; rest = 0.0 }

    @Test
    fun `a source with negative activation sends no input through an inhibitory weight`() = runBlocking {
        val network = Network().apply { timeStep = 1.0 }
        val source = network.addNeuron { updateRule = iacRule(); activation = -1.0; clamped = true }
        val target = network.addNeuron { updateRule = iacRule(); activation = 0.0 }
        network.addSynapse(source, target) { strength = -1.0 }
        network.update()
        assertEquals(0.0, target.activation, 1e-9)
    }

    @Test
    fun `a positively active source inhibits through an inhibitory weight and excites through an excitatory one`() = runBlocking {
        val network = Network().apply { timeStep = 1.0 }
        val source = network.addNeuron { updateRule = iacRule(); activation = 0.5; clamped = true }
        val inhibited = network.addNeuron { updateRule = iacRule(); activation = 0.0 }
        val excited = network.addNeuron { updateRule = iacRule(); activation = 0.0 }
        network.addSynapse(source, inhibited) { strength = -1.0 }
        network.addSynapse(source, excited) { strength = 1.0 }
        network.update()
        assertEquals(-0.5, inhibited.activation, 1e-9)
        assertEquals(0.5, excited.activation, 1e-9)
    }

    @Test
    fun `external input still reaches the neuron`() = runBlocking {
        val network = Network().apply { timeStep = 1.0 }
        val neuron = network.addNeuron { updateRule = iacRule(); activation = 0.0 }
        neuron.addInputValue(0.5)
        network.update()
        assertTrue(neuron.activation > 0.4)
    }
}
