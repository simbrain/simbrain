package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.sin

/**
 * TODO
 *
 *
 * https://en.wikipedia.org/wiki/Kuramoto_model
 *
 *
 * K is weight N = number of fan-in nodes
 *
 *
 * TODO: Contextual increment.  Proper randomize and bounds.
 * Remove un-needed overrides.  Finish GUI.   Include time step in gui.
 */
class KuramotoRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), DifferentiableUpdateRule,
    ClippedUpdateRule, NoisyUpdateRule {
    /**
     * Natural Frequency.
     */
    @UserParameter(label = "Natural frequency", description = "todo.", increment = .1, order = 1)
    var slope: Double = 1.0
        get() = field
        set(slope) {
            field = slope
        }

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
    override var isClipped: Boolean = DEFAULT_CLIPPING

    /**
     * The upper bound of the activity if clipping is used.
     */
    override var upperBound: Double = DEFAULT_UPPER_BOUND

    /**
     * The lower bound of the activity if clipping is used.
     */
    override var lowerBound: Double = DEFAULT_LOWER_BOUND

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {

        var sum = 0.0
        for (s in neuron.fanIn) {
            sum += s.strength * sin(s.source.activation - neuron.activation)
        }
        var N = neuron.fanIn.size.toDouble()
        N = if ((N > 0)) N else 1.0
        val theta_dot = slope + sum / N

        var theta = neuron.activation + (timeStep * theta_dot)
        theta = theta % (2 * Math.PI)

        // if (addNoise) {
        // val += noiseGenerator.nextRand();
        // }
        //
        // if (clipping) {
        // val = clip(val);
        // }
        neuron.activation = theta
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): KuramotoRule {
        val kr = KuramotoRule()
        kr.slope = slope
        kr.isClipped = isClipped
        kr.addNoise = addNoise
        kr.upperBound = upperBound
        kr.lowerBound = lowerBound
        kr.noiseGenerator = noiseGenerator.copy()
        return kr
    }

    override fun getDerivative(`val`: Double): Double {
        return if (`val` >= upperBound) {
            0.0
        } else if (`val` <= lowerBound) {
            0.0
        } else {
            slope
        }
    }

    override val name: String
        get() = "Kuramoto"

    companion object {
        /**
         * The Default upper bound.
         */
        private const val DEFAULT_UPPER_BOUND = 1.0

        /**
         * The Default lower bound.
         */
        private const val DEFAULT_LOWER_BOUND = -1.0

        /**
         * Default clipping setting.
         */
        private const val DEFAULT_CLIPPING = true
    }
}