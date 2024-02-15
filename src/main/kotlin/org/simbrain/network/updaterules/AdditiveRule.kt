package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.atan

/**
 * **AdditiveNeuron** See Haykin (2002), section 14.5. Used with continuous
 * Hopfield networks.
 */
class AdditiveRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), NoisyUpdateRule {
    // TODO: May need clipping and bounds.
    /**
     * Lambda.
     */
    var lambda: Double = 1.4

    /**
     * Resistance.
     */
    var resistance: Double = 1.0

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * For adding noise to the neuron.
     */
    override var addNoise: Boolean = false

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    override fun copy(): AdditiveRule {
        val an = AdditiveRule()
        an.lambda = lambda
        an.resistance = resistance
        an.addNoise = addNoise
        an.noiseGenerator = noiseGenerator.copy()
        return an
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        // Update buffer of additive neuron using Euler's method.

        var wtdSum = 0.0
        if (neuron.fanIn.size > 0) {
            for (j in neuron.fanIn.indices) {
                val w = neuron.fanIn[j]
                val source = w.source
                wtdSum += (w.strength * g(source.activation))
            }
        }

        var `val`: Double =
            neuron.activation + timeStep * (-neuron.activation / resistance + wtdSum)

        if (addNoise) {
            `val` += noiseGenerator.sampleDouble()
        }

        neuron.activation = `val`
        neuron.addInputValue(0.0)
    }

    /**
     * Implements a Hopfield type sigmoidal function.
     *
     * @param x input to function
     * @return output of function
     */
    private fun g(x: Double): Double {
        return 2 / Math.PI * atan((Math.PI * lambda * x) / 2)
    }

    override val name: String
        get() = "Additive (Continuous Hopfield)"
}