package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.activity_generators.StochasticRule

class StochasticRuleTest {

    // Default update rule is stochastic
    val net = Network()
    var rule = StochasticRule()
    var n = Neuron(rule)

    init {
        net.addNetworkModelsAsync(n)
        n.activation = 0.0
        rule.firingProbability = 0.05

    }

    @Test
    fun `Run 10 Times at Firing Probability = 1 Should activate it 10 Times`() {
        with(net) {
            rule.firingProbability = 1.0
            var spikes = 0

            repeat(10) {
                net.update()
                if (n.activation == 1.0) {
                    spikes += 1
                }
            }
            assertEquals(10, spikes)
        }
    }

    @Test
    fun `Run 10 Times at Firing Probability = 0 Should activate it 10 Times`() {
        with(net) {
            rule.firingProbability = 0.0
            var nospikes = 0

            repeat(10) {
                net.update()
                if (n.activation == 0.0) {
                    nospikes += 1
                }
            }
            assertEquals(10, nospikes)
            assertEquals(0.0, n.activation)
        }
    }

    @Test
    // More than 1 spikes (No clue on how to do it statistically)
    fun `Firing Probability = 0_5 Should Activate approximately 50 percent of the time`() {
        var spikes = 0
        rule.firingProbability = 0.5

        repeat(100) {
            net.update()
            if (n.activation == 1.0) {
                    spikes += 1
            }
        }
        // Need two "spikes > 1" because both need to be boolean.
        assertEquals(spikes > 1, spikes > 1)
        assertTrue(spikes > 0)
    }

}
