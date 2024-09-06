package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.addNeuronCollection

class RadialGaussianTest {
    val network = Network()

    @Test
    fun `distConst 0 should make no connection`() {
        val radialGaussian = RadialGaussian(seed = 42L)
        radialGaussian.distConst = 0.0
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = radialGaussian.connectNeurons(neurons, neurons)
                assertEquals(0, connections.size)
            }
        }
    }

    @Test
    fun `distConst 1 should make many connections`() {
        val radialGaussian = RadialGaussian(seed = 42L)
        radialGaussian.distConst = 1.0
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = radialGaussian.connectNeurons(neurons, neurons)
                assert(connections.size > 100)
            }
        }
    }

    @Test
    fun `strategy created with the same seed should produce the same same pattern`() {
        assertStrategiesPatterns(
            network,
            RadialGaussian(seed = 42L),
            RadialGaussian(seed = 42L)
        )
    }


    @Test
    fun `strategy created with different seeds should produce different patterns`() {
        assertStrategiesPatterns(
            network,
            RadialGaussian(seed = 42L),
            RadialGaussian(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `calling connectNeurons on the same strategy object should produce different patterns each time`() {
        val radialGaussian = RadialGaussian(seed = 42L)
        assertStrategiesPatterns(
            network,
            radialGaussian,
            radialGaussian,
            expectIdentical = false
        )
    }
}