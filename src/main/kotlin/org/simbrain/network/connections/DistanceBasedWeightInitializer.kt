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
 * The final weight sign is determined by the polarity assignment from [PolarizedSynapseCollection].
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
    var baseStrength: Double = 1.0

    override fun initializeWeights(polarizedSynapses: PolarizedSynapseCollection) {
        polarizedSynapses.excitatory.forEach { synapse ->
            synapse.forceSetStrength(computeScaledStrength(synapse))
        }
        polarizedSynapses.inhibitory.forEach { synapse ->
            synapse.forceSetStrength(-computeScaledStrength(synapse))
        }
    }

    private fun computeScaledStrength(synapse: Synapse): Double {
        val distance = getEuclideanDist(synapse.source, synapse.target)
        val scalingFactor = decayFunction.getScalingFactor(distance)
        return baseStrength * scalingFactor
    }

    override fun copy(): DistanceBasedWeightInitializer {
        return DistanceBasedWeightInitializer().apply {
            decayFunction = this@DistanceBasedWeightInitializer.decayFunction.copy() as DecayFunction
            baseStrength = this@DistanceBasedWeightInitializer.baseStrength
        }
    }

    override val name = "Distance-Based"

}
