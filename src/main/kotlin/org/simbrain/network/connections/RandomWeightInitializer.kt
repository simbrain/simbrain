package org.simbrain.network.connections

import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.abs
import kotlin.random.Random

/**
 * Initializes synapse weights using probability distributions.
 *
 * Supports separate distributions for excitatory and inhibitory synapses.
 * When randomization is disabled for a polarity, uses the default strength
 * ([DEFAULT_EXCITATORY_STRENGTH] or [DEFAULT_INHIBITORY_STRENGTH]).
 */
class RandomWeightInitializer(seed: Long = Random.nextLong()) : WeightInitializer() {

    /**
     * Whether excitatory connections should be randomized.
     */
    @UserParameter(label = "Randomize Excitatory", description = "Whether to randomize excitatory weights", order = 1)
    var useExcitatoryRandomization = true

    /**
     * Whether inhibitory connections should be randomized.
     */
    @UserParameter(label = "Randomize Inhibitory", description = "Whether to randomize inhibitory weights", order = 2)
    var useInhibitoryRandomization = true

    /**
     * The randomizer for excitatory synapses.
     */
    @UserParameter(label = "Excitatory Randomizer", description = "Distribution for excitatory weights", order = 3, showDetails = false)
    var exRandomizer: ProbabilityDistribution = NormalDistribution().apply { randomSeed = seed }

    /**
     * The randomizer for inhibitory synapses.
     */
    @UserParameter(label = "Inhibitory Randomizer", description = "Distribution for inhibitory weights", order = 4, showDetails = false)
    var inRandomizer: ProbabilityDistribution = NormalDistribution().apply { randomSeed = seed }

    override fun initializeWeights(polarizedSynapses: PolarizedSynapseCollection) {
        polarizedSynapses.excitatory.forEach { synapse ->
            val strength = if (useExcitatoryRandomization) {
                abs(exRandomizer.sampleDouble())
            } else {
                DEFAULT_EXCITATORY_STRENGTH
            }
            synapse.forceSetStrength(strength)
        }

        polarizedSynapses.inhibitory.forEach { synapse ->
            val strength = if (useInhibitoryRandomization) {
                -abs(inRandomizer.sampleDouble())
            } else {
                DEFAULT_INHIBITORY_STRENGTH
            }
            synapse.forceSetStrength(strength)
        }
    }

    override fun copy(): RandomWeightInitializer {
        return RandomWeightInitializer().apply {
            useExcitatoryRandomization = this@RandomWeightInitializer.useExcitatoryRandomization
            useInhibitoryRandomization = this@RandomWeightInitializer.useInhibitoryRandomization
            exRandomizer = this@RandomWeightInitializer.exRandomizer.copy()
            inRandomizer = this@RandomWeightInitializer.inRandomizer.copy()
        }
    }

    override val name = "Random"

}
