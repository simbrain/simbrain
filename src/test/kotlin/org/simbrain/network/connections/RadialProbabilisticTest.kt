package org.simbrain.network.connections

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network

class RadialProbabilisticTest {
    val network = Network()


    @Test
    fun `strategy created with the same seed should produce the same same pattern`() {
        assertStrategiesPatterns(
            network,
            RadialProbabilistic(seed = 42L),
            RadialProbabilistic(seed = 42L)
        )
    }


    @Test
    fun `strategy created with different seeds should produce different patterns`() {
        assertStrategiesPatterns(
            network,
            RadialProbabilistic(seed = 42L),
            RadialProbabilistic(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `calling connectNeurons on the same strategy object should produce different patterns each time`() {
        val radialProbabilistic = RadialProbabilistic(seed = 42L)
        assertStrategiesPatterns(
            network,
            radialProbabilistic,
            radialProbabilistic,
            expectIdentical = false
        )
    }
}