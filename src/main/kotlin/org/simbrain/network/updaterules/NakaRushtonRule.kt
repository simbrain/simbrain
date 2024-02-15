package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.NakaMatrixData
import org.simbrain.network.util.NakaScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.pow

/**
 * **NakaRushtonNeuron** is a firing-rate based neuron which is intended to
 * model spike rates of real neurons. It is used extensively in Hugh Wilson's
 * Spikes, Decisions, and Action. p. 20-21.
 */
class NakaRushtonRule : NeuronUpdateRule<NakaScalarData, NakaMatrixData>(), ClippedUpdateRule, NoisyUpdateRule {
    /**
     * Steepness.
     */
    @UserParameter(
        label = "Steepness",
        description = "This value controls the steepness of the sigmoidal-like function S(W).",
        increment = .1,
        order = 1
    )
    var steepness: Double = 2.0

    /**
     * Semi saturation constant.
     */
    @UserParameter(
        label = "Semi-Saturation Constant",
        description = "This value is the point at which S(W) reaches half of its maximum value.",
        increment = .1,
        order = 2
    )
    var semiSaturationConstant: Double = 120.0

    /**
     * Time constant of spike rate adaptation.
     */
    @UserParameter(
        label = "Adaptation Time Constant",
        description = "This value controls the rate at which the adaptation variable tends to "
                + "its minimum value.",
        increment = .1,
        order = 7
    )
    var adaptationTimeConstant: Double = 1.0

    /**
     * Parameter of spike rate adaptation.
     */
    @UserParameter(
        label = "Adaptation Parameter",
        description = "The parameter of spike rate adaptation.",
        increment = .1,
        order = 6
    )
    var adaptationParameter: Double = .7

    /**
     * Whether to use spike rate adaptation or not.
     */
    @UserParameter(
        label = "Use Adaptation",
        description = "If this is set to true, spike rate adaptation is utilized.",
        increment = .1,
        order = 5
    )
    var useAdaptation: Boolean = false

    /**
     * Time constant.
     */
    @UserParameter(
        label = "Time Constant",
        description = "This value controls the rate at which the activation tends to the fixed "
                + "point S(W).",
        increment = .1,
        order = 3
    )
    var timeConstant: Double = 1.0

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to neuron.
     */
    override var addNoise: Boolean = false

    /**
     * The upper bound of the activity.
     */
    override var upperBound: Double = DEFAULT_UPPER_BOUND.toDouble()

    /**
     * The lower bound of the activity.
     */
    override var lowerBound: Double = 0.0

    /**
     * Bounded update rule is automatically clippable.  It is not needed here since sigmoids automatically respect
     * upper and lower bounds but can still be turned on to constrain contextual increment and decrement.
     */
    override var isClipped: Boolean = false

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    override fun copy(): NakaRushtonRule {
        val rn = NakaRushtonRule()
        rn.steepness = steepness
        rn.semiSaturationConstant = semiSaturationConstant
        rn.upperBound = upperBound
        rn.addNoise = addNoise
        rn.useAdaptation = useAdaptation
        rn.adaptationParameter = adaptationParameter
        rn.adaptationTimeConstant = adaptationTimeConstant
        rn.noiseGenerator = noiseGenerator.copy()
        rn.timeStepSupplier = timeStepSupplier
        return rn
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: NakaMatrixData) {
        for (i in 0 until layer.outputSize()) {
            layer.outputs[i, 0] = nakaRushtonRule(
                layer.inputs[i, 0],
                layer.outputs[i, 0],
                timeStep,
                dataHolder.a[i, 0]
            )
        }
    }

    context(Network)
    override fun apply(neuron: Neuron, data: NakaScalarData) {
        neuron.activation = nakaRushtonRule(
            neuron.input, neuron.activation,
            timeStep, data.a
        )
    }

    fun nakaRushtonRule(input: Double, activation: Double, timeStep: Double, a: Double): Double {
        var a = a
        var `val` = activation

        // Update adaptation term; see Spike, p. 81
        if (useAdaptation) {
            a += (timeStep / adaptationTimeConstant) * (adaptationParameter * `val` - a)
        } else {
            a = 0.0
        }
        val s = if (input > 0) {
            ((upperBound * input.pow(steepness))
                    / ((semiSaturationConstant + a).pow(steepness) + input.pow(steepness)))
        } else {
            0.0
        }

        `val` += if (addNoise) {
            timeStep * (((1 / timeConstant) * (-`val` + s)) + noiseGenerator.sampleDouble())
        } else {
            timeStep * ((1 / timeConstant) * (-`val` + s))
        }

        return `val`
    }

    override fun createMatrixData(size: Int): NakaMatrixData {
        return NakaMatrixData(size)
    }

    override fun createScalarData(): NakaScalarData {
        return NakaScalarData()
    }

    override fun clear(neuron: Neuron) {
        super.clear(neuron)
    }


    override val name: String = "Naka-Rushton"


    companion object {
        /**
         * The default activation ceiling.
         */
        const val DEFAULT_UPPER_BOUND: Int = 100
    }
}