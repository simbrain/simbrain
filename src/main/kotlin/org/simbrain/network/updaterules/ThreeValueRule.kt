package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import java.util.*

/**
 * **ThreeValuedNeuron** is a natural extension of a binary neuron, which
 * takes one of three values depending on the inputs to the neuron in relation
 * to two thresholds.
 */
class ThreeValueRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    /**
     * Bias field.
     */
    @UserParameter(label = "Bias", description = "A fixed amount of input to the node.", increment = .1, order = 1)
    var bias: Double = 0.0

    /**
     * Lower threshold field.
     */
    @JvmField
    @UserParameter(
        label = "Lower Threshold",
        description = "If the weighted input plus the bias is less than this value, the activation "
                + "is set to the Lower Value.",
        increment = .1,
        order = 2
    )
    var lowerThreshold: Double = 0.0

    /**
     * Upper threshold field.
     */
    @JvmField
    @UserParameter(
        label = "Upper Threshold",
        description = "If the weighted input plus the bias is greater than this value, the activation "
                + "is set to the Upper Value.",
        increment = .1,
        order = 3
    )
    var upperThreshold: Double = 1.0

    /**
     * Lower value field.
     */
    @JvmField
    @UserParameter(
        label = "Lower Value",
        description = "The activation is set to this value if the weighted input plus the bias exceeds "
                + "the Lower Threshold.",
        increment = .1,
        order = 4
    )
    var lowerValue: Double = -1.0

    /**
     * Middle value field.
     */
    @JvmField
    @UserParameter(
        label = "Middle Value",
        description = "If the weighted input plus the bias does not exceed the Upper or Lower Thresholds, "
                + "then the activation is set to this value.",
        increment = .1,
        order = 5
    )
    var middleValue: Double = 0.0

    /**
     * Upper value field.
     */
    @JvmField
    @UserParameter(
        label = "Upper Value", description = "If the weighted input plus the bias is greater than the Upper Threshold, "
                + "then the activation is set to this value.", increment = .1, order = 6
    )
    var upperValue: Double = 1.0

    override val timeType: Network.TimeType
        /**
         * {@inheritDoc}
         */
        get() = Network.TimeType.DISCRETE

    override fun deepCopy(): ThreeValueRule {
        val tv = ThreeValueRule()
        tv.bias = bias
        tv.lowerThreshold = lowerThreshold
        tv.upperThreshold = upperThreshold
        tv.lowerValue = lowerValue
        tv.middleValue = middleValue
        tv.upperValue = upperValue

        return tv
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        val wtdInput = neuron.input

        if (wtdInput < lowerThreshold) {
            neuron.activation = lowerValue
        } else if (wtdInput > upperThreshold) {
            neuron.activation = upperValue
        } else {
            neuron.activation = middleValue
        }
    }

    override val randomValue: Double
        get() {
            val rand = Random()
            val d = rand.nextInt(3)
            return if (d == 0) {
                lowerValue
            } else if (d == 1) {
                middleValue
            } else {
                upperValue
            }
        }

    override val name: String = "Three Value"

    override val graphicalLowerBound: Double
        get() = lowerValue - 1

    override val graphicalUpperBound: Double
        get() = upperValue + 1
}