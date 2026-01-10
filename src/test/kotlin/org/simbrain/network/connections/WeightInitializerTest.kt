package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Neuron
import org.simbrain.util.SimbrainConstants.Polarity

class WeightInitializerTest {

    @Test
    fun `RandomWeightInitializer should randomize weights`() {
        // Create neurons with BOTH polarity
        val sources = List(5) { Neuron().apply { polarity = Polarity.BOTH } }
        val targets = List(5) { Neuron() }

        // Create FixedDegree connection strategy
        val fixedDegree = FixedDegree(degree = 3, direction = Direction.OUT)
        fixedDegree.percentExcitatory = 50.0

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
    fun `RandomWeightInitializer initializeWeights should change values`() {
        // Create neurons
        val source = Neuron().apply { polarity = Polarity.BOTH }
        val target = Neuron()

        // Create a synapse with strength 1.0
        val synapse = org.simbrain.network.core.Synapse(source, target)
        println("Initial synapse strength: ${synapse.strength}")

        // Create RandomWeightInitializer
        val initializer = RandomWeightInitializer()
        println("useExcitatoryRandomization: ${initializer.useExcitatoryRandomization}")
        println("exRandomizer type: ${initializer.exRandomizer::class.simpleName}")

        // Sample from the randomizer directly
        val sample1 = initializer.exRandomizer.sampleDouble()
        val sample2 = initializer.exRandomizer.sampleDouble()
        println("Direct samples from exRandomizer: $sample1, $sample2")

        // Initialize weights
        initializer.initializeWeights(listOf(synapse))
        println("After initializeWeights: ${synapse.strength}")

        // The weight should have changed from 1.0
        assertNotEquals(1.0, synapse.strength, "Weight should be randomized")
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
