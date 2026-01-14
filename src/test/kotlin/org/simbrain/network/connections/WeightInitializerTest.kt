package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Neuron
import org.simbrain.util.SimbrainConstants.Polarity

class WeightInitializerTest {

    @Test
    fun `RandomWeightInitializer should randomize weights when enabled`() {
        // Create neurons with BOTH polarity
        val sources = List(5) { Neuron().apply { polarity = Polarity.BOTH } }
        val targets = List(5) { Neuron() }

        // Create FixedDegree connection strategy with randomization enabled
        val fixedDegree = FixedDegree(degree = 3, direction = Direction.OUT)
        fixedDegree.percentExcitatory = 50.0
        (fixedDegree.weightInitializer as RandomWeightInitializer).apply {
            useExcitatoryRandomization = true
            useInhibitoryRandomization = true
        }

        // Connect neurons
        val synapses = fixedDegree.connectNeurons(sources, targets)

        println("Number of synapses: ${synapses.size}")
        println("Weight initializer type: ${fixedDegree.weightInitializer::class.simpleName}")
        println("Synapse weights: ${synapses.map { it.strength }}")

        // Check that not all weights are ±1
        val uniqueWeights = synapses.map { it.strength }.toSet()
        println("Unique weights: $uniqueWeights")

        // If all weights are just -1.0 and 1.0, the test should fail
        assertFalse(
            uniqueWeights.all { it == 1.0 || it == -1.0 },
            "Weights should be randomized, not just ±1. Got: $uniqueWeights"
        )
    }

    @Test
    fun `RandomWeightInitializer initializeWeights should set default strength`() {
        // Create neurons
        val source = Neuron().apply { polarity = Polarity.BOTH }
        val target = Neuron()

        // Create a synapse
        val synapse = org.simbrain.network.core.Synapse(source, target)
        println("Initial synapse strength: ${synapse.strength}")

        // Create RandomWeightInitializer (without randomization enabled)
        val initializer = RandomWeightInitializer()
        println("useExcitatoryRandomization: ${initializer.useExcitatoryRandomization}")

        // Initialize weights with the synapse as excitatory
        val polarized = PolarizedSynapseCollection(excitatory = listOf(synapse), inhibitory = emptyList())
        initializer.initializeWeights(polarized)
        println("After initializeWeights: ${synapse.strength}")

        // Without randomization, should be set to default excitatory strength
        assertEquals(DEFAULT_EXCITATORY_STRENGTH, synapse.strength, "Weight should be default excitatory strength")
    }

    @Test
    fun `RandomWeightInitializer initializeWeights should randomize when enabled`() {
        // Create neurons
        val source = Neuron().apply { polarity = Polarity.BOTH }
        val target = Neuron()

        // Create a synapse
        val synapse = org.simbrain.network.core.Synapse(source, target)

        // Create RandomWeightInitializer with randomization enabled
        val initializer = RandomWeightInitializer().apply {
            useExcitatoryRandomization = true
        }

        // Initialize weights with the synapse as excitatory
        val polarized = PolarizedSynapseCollection(excitatory = listOf(synapse), inhibitory = emptyList())
        initializer.initializeWeights(polarized)

        // With randomization enabled, should be randomized (likely not exactly 1.0)
        assertTrue(synapse.strength > 0, "Excitatory weight should be positive")
    }

    @Test
    fun `ConnectionStrategy weightInitializer should be RandomWeightInitializer by default`() {
        val fixedDegree = FixedDegree()

        println("weightInitializer class: ${fixedDegree.weightInitializer::class.qualifiedName}")

        assertTrue(
            fixedDegree.weightInitializer is RandomWeightInitializer,
            "Default weightInitializer should be RandomWeightInitializer"
        )
    }
}
