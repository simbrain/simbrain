package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.SimbrainConstants.Polarity

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

    @Test
    fun `EXCITATORY source neurons should produce positive weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val radialProb = RadialProbabilistic(
            excitatoryProbability = 0.8,
            excitatoryRadius = 100.0,
            seed = 42L
        )
        
        val syns = radialProb.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive after fix, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY source neurons should produce negative weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val radialProb = RadialProbabilistic(
            excitatoryProbability = 0.8,
            excitatoryRadius = 100.0,
            seed = 42L
        )
        
        val syns = radialProb.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative after fix, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `mixed polarity sources should respect source polarity`() {
        val excitatoryNeurons = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 15.0
            y = 0.0
        }}
        val inhibitoryNeurons = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 15.0 + 7.5
            y = 0.0
        }}
        val sources = excitatoryNeurons + inhibitoryNeurons
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val radialProb = RadialProbabilistic(
            excitatoryProbability = 0.8,
            excitatoryRadius = 100.0,
            seed = 42L
        )
        
        val syns = radialProb.connectNeurons(sources, targets)
        
        val excitatorySourceSyns = syns.filter { it.source in excitatoryNeurons }
        val inhibitorySourceSyns = syns.filter { it.source in inhibitoryNeurons }
        
        assertTrue(excitatorySourceSyns.all { it.strength > 0 }) {
            "Synapses from EXCITATORY sources should be positive"
        }
        assertTrue(inhibitorySourceSyns.all { it.strength < 0 }) {
            "Synapses from INHIBITORY sources should be negative"
        }
    }
}