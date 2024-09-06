package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.addNeuron
import org.simbrain.util.decayfunctions.LinearDecayFunction

class DistancedBasedTest {

    val network = Network()

    @Test
    fun `check that connectNeurons produces the appropriate number of synapses`() {
        runBlocking {
            with(network) {
                val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(80.0), seed = 42L)
                val n1 = addNeuron(0, 0)
                val n2 = addNeuron(0, 40)
                val count = (0..1000).sumOf {
                    distanceBased.connectNeurons(listOf(n1), listOf(n2)).size
                }
                assert(count in 450..550)
            }
        }
    }

    @Test
    fun `strategy created with the same seed should produce the same same pattern`() {
        assertStrategiesPatterns(
            network,
            DistanceBased(seed = 42L),
            DistanceBased(seed = 42L),
            25
        )
    }


    @Test
    fun `strategy created with different seeds should produce different patterns`() {
        assertStrategiesPatterns(
            network,
            DistanceBased(seed = 42L),
            DistanceBased(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `calling connectNeurons on the same strategy object should produce different patterns each time`() {
        val distanceBased = DistanceBased(seed = 42L)
        assertStrategiesPatterns(
            network,
            distanceBased,
            distanceBased,
            expectIdentical = false
        )
    }

}