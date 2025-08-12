package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.activity_generators.StochasticRule

class StochasticRuleTest {

    val net = Network()
    var rule = StochasticRule()
    var n = Neuron(rule)

    init {
        net.addNetworkModelsAsync(n)
    }

    @Test
    fun `Should always spike when firing probability is 1`() {
        with(net) {
            rule.firingProbability = 1.0
            var spikes = 0
            repeat(10) {
                net.update()
                if (n.isSpike) {
                    spikes += 1
                }
            }
            assertEquals(10, spikes)
        }
    }

    @Test
    fun `Should never spike when firing probability is 0`() {
        with(net) {
            rule.firingProbability = 0.0
            var spikes = 0
            repeat(10) {
                net.update()
                if (n.isSpike) {
                    spikes += 1
                }
            }
            assertEquals(0, spikes)
        }
    }

    @Test
    fun `Spike at least once with firing probability of one half and enough iterations`() {
        with(net) {
            rule.firingProbability = 0.5
            var spikes = 0
            repeat(50) {
                net.update()
                if (n.isSpike) {
                    spikes += 1
                }
            }
            assertTrue(spikes > 0)
        }
    }

}
