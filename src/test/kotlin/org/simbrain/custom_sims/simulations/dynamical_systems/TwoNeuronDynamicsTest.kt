/** Checks the recurrent simulation's map, periodic presets, and numerical sensitivity using network updates. */
package org.simbrain.custom_sims.simulations.dynamical_systems

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.tanh

class TwoNeuronDynamicsTest {
    @Test
    fun `network updates implement the synchronous tanh map`() = runBlocking {
        val network = Network()
        val neurons = network.addTwoNeuronDynamicsNetwork().neuronList
        repeat(30) {
            val first = neurons[0].activation
            val second = neurons[1].activation
            network.update()
            assertEquals(tanh(4.43 * first - 4.74 * second), neurons[0].activation, 1e-12)
            assertEquals(tanh(1.97 * first - 1.10 * second), neurons[1].activation, 1e-12)
        }
    }

    @Test
    fun `periodic presets settle into their advertised minimal periods`() = runBlocking {
        for ((gain, period) in listOf(0.3 to 1, 0.8 to 6, 1.04 to 5, 1.07 to 10, 1.09 to 20)) {
            val network = Network()
            val neurons = network.addTwoNeuronDynamicsNetwork()
            configureTwoNeuronDynamics(neurons, gain)
            repeat(3000) { network.update() }
            val states = List(200) {
                network.update()
                neurons.neuronList.map { it.activation }
            }
            val measuredPeriod = (1..40).firstOrNull { lag ->
                (lag until states.size).all { index ->
                    states[index].indices.all { dimension ->
                        abs(states[index][dimension] - states[index - lag][dimension]) < 1e-8
                    }
                }
            }
            assertEquals(period, measuredPeriod, "Gain $gain")
        }
    }

    @Test
    fun `chaotic preset has positive estimated largest Lyapunov exponent`() = runBlocking {
        val network = Network()
        val neurons = network.addTwoNeuronDynamicsNetwork().neuronList
        var tangentFirst = 1.0
        var tangentSecond = 0.0
        var logStretch = 0.0
        repeat(12000) { step ->
            network.update()
            val first = neurons[0].activation
            val second = neurons[1].activation
            assertTrue(first.isFinite() && first in -1.0..1.0)
            assertTrue(second.isFinite() && second in -1.0..1.0)
            val nextFirst = (1 - first * first) * (4.43 * tangentFirst - 4.74 * tangentSecond)
            val nextSecond = (1 - second * second) * (1.97 * tangentFirst - 1.10 * tangentSecond)
            val stretch = hypot(nextFirst, nextSecond)
            tangentFirst = nextFirst / stretch
            tangentSecond = nextSecond / stretch
            if (step >= 2000) logStretch += ln(stretch)
        }
        val exponent = logStretch / 10000
        assertTrue(exponent > 0.1, "Estimated exponent: $exponent")
    }
}
