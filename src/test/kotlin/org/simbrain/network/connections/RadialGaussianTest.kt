package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.util.SimbrainConstants.Polarity

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

    @Test
    fun `EXCITATORY neurons should produce positive weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val radialGaussian = RadialGaussian(distConst = 0.5, seed = 42L)
        
        val syns = radialGaussian.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY neurons should produce negative weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val radialGaussian = RadialGaussian(distConst = 0.5, seed = 42L)
        
        val syns = radialGaussian.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `EE connections should respect excitatory polarity`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 20.0
            y = 0.0
        }}
        val targets = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 20.0
            y = 50.0
        }}
        val radialGaussian = RadialGaussian(eeDistConst = 0.8, seed = 42L)
        
        val syns = radialGaussian.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 })
    }

    @Test
    fun `II connections should respect inhibitory polarity`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 20.0
            y = 0.0
        }}
        val targets = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 20.0
            y = 50.0
        }}
        val radialGaussian = RadialGaussian(iiDistConst = 0.8, seed = 42L)
        
        val syns = radialGaussian.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength < 0 })
    }

    @Test
    fun `EI connections should respect source excitatory polarity`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 20.0
            y = 0.0
        }}
        val targets = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 20.0
            y = 50.0
        }}
        val radialGaussian = RadialGaussian(eiDistConst = 0.8, seed = 42L)
        
        val syns = radialGaussian.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 })
    }

    @Test
    fun `IE connections should respect source inhibitory polarity`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 20.0
            y = 0.0
        }}
        val targets = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 20.0
            y = 50.0
        }}
        val radialGaussian = RadialGaussian(ieDistConst = 0.8, seed = 42L)
        
        val syns = radialGaussian.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength < 0 })
    }
}