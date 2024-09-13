package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.abs

/**
 * **DecayNeuron** implements various forms of standard decay.
 */
open class DecayRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), ClippedUpdateRule, NoisyUpdateRule {

    enum class UpdateType {
        Relative, Absolute
    }

    @UserParameter(
        label = "Update Type",
        description = "Relative (percentage decay of current activation) vs. absolute (fixed decay amount)",
        order = 10
    )
    var updateType: UpdateType = UpdateType.Relative

    var decayAmount by GuiEditable(
        initValue = .1,
        label = "Decay amount",
        description = "The amount by which the activation is changed each iteration if absolute decay is used.",
        onUpdate = { enableWidget(widgetValue(DecayRule::updateType) == UpdateType.Absolute) },
        order = 20
    )

    var decayFraction by GuiEditable(
        initValue = .1,
        label = "Decay fraction",
        description = "The proportion of the distance between the current value and the base-line value, "
                + "by which the activation is changed each iteration if relative decay is chosen.",
        onUpdate = { enableWidget(widgetValue(DecayRule::updateType) == UpdateType.Relative) },
        order = 30
    )

    @UserParameter(
        label = "Base Line",
        description = "An option to add noise.",
        increment = .1,
        order = 40)
    var baseLine: Double = 0.0

    /**
     * Clipping.
     */
    override var isClipped: Boolean = true

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to the neuron.
     */
    override var addNoise: Boolean = false

    /**
     * The upper bound of the activity if clipping is used.
     */
    override var upperBound: Double = DEFAULT_CEILING

    /**
     * The lower bound of the activity if clipping is used.
     */
    override var lowerBound: Double = DEFAULT_FLOOR

    context(Network)
    override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        for (i in 0 until layer.activations.nrow()) {
            layer.activations[i, 0] = decayRule(
                layer.inputs[i, 0],
                layer.activations[i, 0]
            )
        }
        clip(layer.activations)
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        neuron.activation = decayRule(
            neuron.input,
            neuron.activation
        )
        neuron.clip()
    }

    private fun decayRule(input: Double, activation: Double): Double {
        var retVal = input + activation
        var decayVal = 0.0
        decayVal = if (updateType == UpdateType.Relative) {
            decayFraction * abs(retVal - baseLine)
        } else {
            decayAmount
        }
        if (retVal < baseLine) {
            retVal += decayVal
            if (retVal > baseLine) {
                retVal = baseLine
            }
        } else if (retVal > baseLine) {
            retVal -= decayVal
            if (retVal < baseLine) {
                retVal = baseLine
            }
        }
        if (addNoise) {
            retVal += noiseGenerator.sampleDouble()
        }
        return retVal
    }

    override fun createMatrixData(size: Int): EmptyMatrixData {
        return EmptyMatrixData
    }

    override fun createScalarData(): EmptyScalarData {
        return EmptyScalarData
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): DecayRule {
        val dn = DecayRule()
        dn.updateType = updateType
        dn.decayAmount = decayAmount
        dn.decayFraction = decayFraction
        dn.isClipped = isClipped
        dn.upperBound = upperBound
        dn.lowerBound = lowerBound
        dn.addNoise = addNoise
        dn.noiseGenerator = noiseGenerator.copy()
        return dn
    }

    override val name: String
        get() = "Decay"

    companion object {
        /**
         * The Default upper bound.
         */
        private const val DEFAULT_CEILING = 1.0

        /**
         * The Default lower bound.
         */
        private const val DEFAULT_FLOOR = -1.0
    }
}