package org.simbrain.network.connections

import org.simbrain.network.core.Synapse
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.random.Random

/**
 * Initializes synapse weights using probability distributions.
 *
 * Supports separate distributions for excitatory and inhibitory synapses,
 * determined by the source neuron's polarity.
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

    override fun initializeWeights(synapses: List<Synapse>) {
        synapses.forEach { synapse ->
            // Use source neuron polarity if set, otherwise use synapse's current strength sign
            // This allows polarizeSynapses to set the polarity before randomization
            val isExcitatory = when (synapse.source.polarity) {
                Polarity.EXCITATORY -> true
                Polarity.INHIBITORY -> false
                Polarity.BOTH -> synapse.strength >= 0
            }

            if (isExcitatory) {
                if (useExcitatoryRandomization) {
                    // For excitatory: ensure positive weight
                    val sampledValue = exRandomizer.sampleDouble()
                    synapse.forceSetStrength(kotlin.math.abs(sampledValue))
                }
            } else {
                if (useInhibitoryRandomization) {
                    // For inhibitory: ensure negative weight
                    val sampledValue = inRandomizer.sampleDouble()
                    synapse.forceSetStrength(-kotlin.math.abs(sampledValue))
                }
            }
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
