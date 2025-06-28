package org.simbrain.network.updaterules.activity_generators

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.ActivityGenerator
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * Produces random activations within specified parameters.
 */
@CustomTypeName("Random Activity Generator")
class RandomNeuronRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>, ActivityGenerator, ClippedUpdateRule, NoisyUpdateRule {
    /**
     * Noise source.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    override var upperBound: Double = 1.0

    override var lowerBound: Double = -1.0

    /**
     * Bounded update rule is automatically clippable.  It is not needed here since sigmoids automatically respect
     * upper and lower bounds but can still be turned on to constrain contextual increment and decrement.
     */
    override var isClipped: Boolean = false

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    constructor() : super()

    constructor(rn: RandomNeuronRule, n: Neuron?) : super() {
        noiseGenerator = rn.noiseGenerator.copy()
    }

    override fun copy(): RandomNeuronRule {
        val rn = RandomNeuronRule()
        rn.noiseGenerator = noiseGenerator.copy()
        return rn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        neuron.activation = noiseGenerator.sampleDouble()
    }

    override val name: String
        get() = "Random"

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double = noiseGenerator.sampleDouble()

    override var addNoise: Boolean
        get() = true
        set(noise) {
        }
}
