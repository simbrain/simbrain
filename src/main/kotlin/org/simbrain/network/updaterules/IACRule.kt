package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * **IACNeuron** implements an Interactive Activation and Competition neuron.
 */
class IACRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), ClippedUpdateRule, NoisyUpdateRule {
    @UserParameter(
        label = "Decay Rate",
        description = "The rate at which activation decays to its resting value.",
        increment = .1,
        order = 1
    )
    var decay: Double = 0.05

    @UserParameter(
        label = "Rest",
        description = "The resting value which the activation decays to.",
        increment = .1,
        order = 2
    )
    var rest: Double = .1

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to the neuron.
     */
    override var addNoise: Boolean = false

    /**
     * Clipping.
     */
    override var isClipped: Boolean = true

    /**
     * The upper bound of the activity if clipping is used.
     */
    override var upperBound: Double = DEFAULT_CEILING

    /**
     * The lower bound of the activity if clipping is used.
     */
    override var lowerBound: Double = DEFAULT_FLOOR

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun deepCopy(): IACRule {
        val iac = IACRule()
        iac.decay = decay
        iac.rest = rest
        iac.isClipped = isClipped
        iac.upperBound = upperBound
        iac.lowerBound = lowerBound
        iac.addNoise = addNoise
        iac.noiseGenerator = noiseGenerator.deepCopy()
        return iac
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        // Notation and algorithm from McClelland 1981, Proceedings of the third
        // annual cog-sci meeting

        // Sum of the "active excitors" and "active inhibitors"

        var netInput = neuron.input
        for (w in neuron.fanIn) {
            if (w.source.activation > 0) {
                netInput += (w.strength * w.source.activation)
            }
        }

        var effect = 0.0
        effect = if (netInput >= 0) {
            (upperBound - neuron.activation) * netInput
        } else {
            (neuron.activation - lowerBound) * netInput
        }

        // Update activation using Euler integration of main ODE
        var act: Double =
            neuron.activation + timeStep * (effect - decay * (neuron.activation - rest))

        if (addNoise) {
            act += noiseGenerator.sampleDouble()
        }

        neuron.activation = act
        neuron.clip()
    }

    override val name: String
        get() = "IAC"

    companion object {
        /**
         * The Default upper bound.
         */
        private const val DEFAULT_CEILING = 1.0

        /**
         * The Default lower bound.
         */
        private const val DEFAULT_FLOOR = -.2
    }
}