package org.simbrain.network.updaterules.activity_generators

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.ActivityGenerator
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CustomTypeName

/**
 * Produces spikes using a user settable probability.
 */
@CustomTypeName("Stochastic Spike Generator")
class StochasticRule : SpikingNeuronUpdateRule<SpikingScalarData, SpikingMatrixData>(), ActivityGenerator {

    @UserParameter(
        label = "Firing Probability",
        description = "This parameter determines the probability that the generator will fire, "
                + "causing it to have an activation equal to its upper bound, given an iteration.",
        order = 1
    )
    var firingProbability: Double = DEFAULT_FIRING_PROBABILITY

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): StochasticRule {
        val sn = StochasticRule()
        sn.firingProbability = firingProbability
        return sn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: SpikingScalarData) {
        val rand = Math.random()
        if (rand > 1 - firingProbability) {
            neuron.isSpike = true
            neuron.activation = 1.0
        } else {
            neuron.isSpike = false
            neuron.activation = 0.0 // Make this a separate variable?
        }
    }

    override val name: String
        get() = "Stochastic"

    companion object {
        /**
         * The default firing probability for the Neuron.
         */
        private const val DEFAULT_FIRING_PROBABILITY = .05
    }
}
