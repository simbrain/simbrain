package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.decayfunctions.StepDecayFunction

class DistanceBasedTest {
    val network = Network()

    // Basic DistanceBased tests.

    @Test
    fun `strategy created with the same seed should produce the same pattern`() {
        assertStrategiesPatterns(
            network,
            DistanceBased(seed = 42L),
            DistanceBased(seed = 42L)
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
        val strategy = DistanceBased(seed = 42L)
        assertStrategiesPatterns(
            network,
            strategy,
            strategy,
            expectIdentical = false
        )
    }

    @Test
    fun `baseMultiplier 0 should make no connections`() {
        val strategy = DistanceBased(
            decayFunction = GaussianDecayFunction().apply { baseMultiplier = 0.0 },
            seed = 42L
        )
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = strategy.connectNeurons(neurons, neurons)
                assertEquals(0, connections.size)
            }
        }
    }

    @Test
    fun `baseMultiplier 1 should make many connections`() {
        val strategy = DistanceBased(
            decayFunction = GaussianDecayFunction(200.0).apply { baseMultiplier = 1.0 },
            seed = 42L
        )
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = strategy.connectNeurons(neurons, neurons)
                assertTrue(connections.size > 100) {
                    "Expected more than 100 connections, got ${connections.size}"
                }
            }
        }
    }

    // radialGaussianStyle adapter tests.

    @Test
    fun `radialGaussianStyle with distConst 0 should make no connections`() {
        val strategy = radialGaussianStyle(
            distConst = 0.0,
            eeDistConst = 0.0,
            eiDistConst = 0.0,
            ieDistConst = 0.0,
            iiDistConst = 0.0,
            seed = 42L
        )
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = strategy.connectNeurons(neurons, neurons)
                assertEquals(0, connections.size)
            }
        }
    }

    @Test
    fun `radialGaussianStyle with high distConst should make many connections`() {
        val strategy = radialGaussianStyle(distConst = 1.0, seed = 42L)
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = strategy.connectNeurons(neurons, neurons)
                assertTrue(connections.size > 50)
            }
        }
    }

    @Test
    fun `radialGaussianStyle should produce same pattern with same seed`() {
        assertStrategiesPatterns(
            network,
            radialGaussianStyle(seed = 42L),
            radialGaussianStyle(seed = 42L)
        )
    }

    @Test
    fun `radialGaussianStyle should produce different pattern with different seeds`() {
        assertStrategiesPatterns(
            network,
            radialGaussianStyle(seed = 42L),
            radialGaussianStyle(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `radialGaussianStyle EXCITATORY neurons should produce positive weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val strategy = radialGaussianStyle(distConst = 0.5, seed = 42L)

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `radialGaussianStyle INHIBITORY neurons should produce negative weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val strategy = radialGaussianStyle(distConst = 0.5, seed = 42L)

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `radialGaussianStyle EE connections should respect excitatory polarity`() {
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
        val strategy = radialGaussianStyle(eeDistConst = 0.8, seed = 42L)

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 })
    }

    @Test
    fun `radialGaussianStyle II connections should respect inhibitory polarity`() {
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
        val strategy = radialGaussianStyle(iiDistConst = 0.8, seed = 42L)

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength < 0 })
    }

    @Test
    fun `radialGaussianStyle EI connections should respect source excitatory polarity`() {
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
        val strategy = radialGaussianStyle(eiDistConst = 0.8, seed = 42L)

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 })
    }

    @Test
    fun `radialGaussianStyle IE connections should respect source inhibitory polarity`() {
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
        val strategy = radialGaussianStyle(ieDistConst = 0.8, seed = 42L)

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength < 0 })
    }

    // radialProbabilisticStyle adapter tests.

    @Test
    fun `radialProbabilisticStyle should produce same pattern with same seed`() {
        assertStrategiesPatterns(
            network,
            radialProbabilisticStyle(seed = 42L),
            radialProbabilisticStyle(seed = 42L)
        )
    }

    @Test
    fun `radialProbabilisticStyle should produce different pattern with different seeds`() {
        assertStrategiesPatterns(
            network,
            radialProbabilisticStyle(seed = 42L),
            radialProbabilisticStyle(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `radialProbabilisticStyle EXCITATORY source neurons should produce positive weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val strategy = radialProbabilisticStyle(
            radius = 100.0,
            probability = 0.8,
            seed = 42L
        )

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `radialProbabilisticStyle INHIBITORY source neurons should produce negative weights`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val strategy = radialProbabilisticStyle(
            radius = 100.0,
            probability = 0.8,
            seed = 42L
        )

        val syns = strategy.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative, found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `radialProbabilisticStyle mixed polarity sources should respect source polarity`() {
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
        val strategy = radialProbabilisticStyle(
            radius = 100.0,
            probability = 0.8,
            seed = 42L
        )

        val syns = strategy.connectNeurons(sources, targets)

        val excitatorySourceSyns = syns.filter { it.source in excitatoryNeurons }
        val inhibitorySourceSyns = syns.filter { it.source in inhibitoryNeurons }

        assertTrue(excitatorySourceSyns.all { it.strength > 0 }) {
            "Synapses from EXCITATORY sources should be positive"
        }
        assertTrue(inhibitorySourceSyns.all { it.strength < 0 }) {
            "Synapses from INHIBITORY sources should be negative"
        }
    }

    @Test
    fun `radialProbabilisticStyle probability 0 should make no connections`() {
        val strategy = radialProbabilisticStyle(
            radius = 100.0,
            probability = 0.0,
            seed = 42L
        )
        runBlocking {
            with(network) {
                val neurons = addNeuronCollection(25).neuronList
                val connections = strategy.connectNeurons(neurons, neurons)
                assertEquals(0, connections.size)
            }
        }
    }

    @Test
    fun `radialProbabilisticStyle small radius should make fewer connections than large radius`() {
        runBlocking {
            with(network) {
                val neurons1 = addNeuronCollection(25).neuronList
                val neurons2 = addNeuronCollection(25).neuronList

                val smallRadius = radialProbabilisticStyle(radius = 30.0, probability = 1.0, seed = 42L)
                val largeRadius = radialProbabilisticStyle(radius = 200.0, probability = 1.0, seed = 42L)

                val smallConnections = smallRadius.connectNeurons(neurons1, neurons1)
                val largeConnections = largeRadius.connectNeurons(neurons2, neurons2)

                assertTrue(smallConnections.size < largeConnections.size) {
                    "Small radius (${smallConnections.size}) should produce fewer connections than large radius (${largeConnections.size})"
                }
            }
        }
    }

    // StepDecayFunction tests.

    @Test
    fun `StepDecayFunction should return baseMultiplier inside radius`() {
        val step = StepDecayFunction(100.0).apply { baseMultiplier = 0.5 }
        assertEquals(0.5, step.getScalingFactor(50.0))
        assertEquals(0.5, step.getScalingFactor(99.0))
    }

    @Test
    fun `StepDecayFunction should return 0 outside radius`() {
        val step = StepDecayFunction(100.0).apply { baseMultiplier = 0.5 }
        assertEquals(0.0, step.getScalingFactor(101.0))
        assertEquals(0.0, step.getScalingFactor(200.0))
    }

    // GaussianDecayFunction baseMultiplier tests.

    @Test
    fun `GaussianDecayFunction baseMultiplier should scale output`() {
        val gaussian1 = GaussianDecayFunction(100.0).apply { baseMultiplier = 1.0 }
        val gaussian05 = GaussianDecayFunction(100.0).apply { baseMultiplier = 0.5 }

        val distance = 50.0
        val value1 = gaussian1.getScalingFactor(distance)
        val value05 = gaussian05.getScalingFactor(distance)

        assertEquals(value1 * 0.5, value05, 0.0001) {
            "baseMultiplier 0.5 should produce half the value"
        }
    }
}
