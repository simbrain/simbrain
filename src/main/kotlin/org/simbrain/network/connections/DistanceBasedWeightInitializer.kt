package org.simbrain.network.connections

import org.simbrain.network.core.Synapse
import org.simbrain.network.core.getEuclideanDist
import org.simbrain.util.UserParameter
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.GaussianDecayFunction

/**
 * Initializes synapse weights based on the distance between source and target neurons.
 *
 * Closer neurons receive stronger weights (scaled by the decay function).
 * The final weight respects the source neuron's polarity (excitatory = positive, inhibitory = negative).
 */
class DistanceBasedWeightInitializer : WeightInitializer() {

    /**
     * The decay function that determines how weight scales with distance.
     * Returns a scaling factor between 0 and 1.
     */
    @UserParameter(label = "Decay Function", description = "How weight strength decays with distance", order = 1)
    var decayFunction: DecayFunction = GaussianDecayFunction()

    /**
     * The maximum weight strength (at distance = 0 or peak distance).
     * This is scaled by the decay function based on distance.
     */
    @UserParameter(label = "Base Strength", description = "Maximum weight strength at peak distance", order = 2)
    var baseStrength: Double = 10.0

    override fun initializeWeights(synapses: List<Synapse>) {
        synapses.forEach { synapse ->
            val distance = getEuclideanDist(synapse.source, synapse.target)
            val scalingFactor = decayFunction.getScalingFactor(distance)
            val scaledStrength = baseStrength * scalingFactor
            synapse.forceSetStrength(synapse.source.polarity.value(scaledStrength))
        }
    }

    override fun copy(): DistanceBasedWeightInitializer {
        return DistanceBasedWeightInitializer().apply {
            decayFunction = this@DistanceBasedWeightInitializer.decayFunction.copy() as DecayFunction
            baseStrength = this@DistanceBasedWeightInitializer.baseStrength
        }
    }

    override val name = "Distance-Based"

}
