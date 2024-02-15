package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.UserParameter
import java.util.*

/**
 * **BinaryNeuron** takes one of two values.
 */
class BinaryRule : NeuronUpdateRule<BiasedScalarData, BiasedMatrixData> {
    /**
     * Threshold for binary neurons.
     */
    @UserParameter(label = "Threshold", description = "Threshold for binary neurons.", increment = .1, order = 1)
    var threshold: Double = .5

    // TODO: Get rid of redundant wording.   Have not cleaned it up yet
    // for fear of xstream problems.
    @UserParameter(label = "On Value", description = "The value that turns on neuron.", increment = .1, order = 2)
    var upperBound: Double = DEFAULT_CEILING

    @UserParameter(label = "Off Value", description = "The value that turns off neuron.", increment = .1, order = 3)
    var lowerBound: Double = DEFAULT_FLOOR

    /**
     * Bias for binary neurons.
     */
    @UserParameter(label = "Bias", description = "Bias for binary neurons.", increment = .1, order = 4)
    var bias: Double = 0.0

    constructor()

    constructor(floor: Double, ceil: Double, threshold: Double) {
        this.upperBound = ceil
        this.lowerBound = floor
        this.threshold = threshold
    }

    override fun copy(): BinaryRule {
        val bn = BinaryRule()
        bn.threshold = threshold
        bn.setCeiling(upperBound)
        bn.setFloor(lowerBound)
        return bn
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: BiasedMatrixData) {
        for (i in 0 until layer.outputs.nrow()) {
            layer.outputs[i, 0] = binaryRule(layer.inputs[i, 0], dataHolder.biases[i, 0])
        }
    }

    context(Network)
    override fun apply(neuron: Neuron, data: BiasedScalarData) {
        neuron.activation = binaryRule(neuron.input, data.bias)
    }

    fun binaryRule(`in`: Double, bias: Double): Double {
        val wtdInput = `in` + bias
        return if (wtdInput > threshold) {
            upperBound
        } else {
            lowerBound
        }
    }

    override fun createMatrixData(size: Int): BiasedMatrixData {
        return BiasedMatrixData(size)
    }

    override fun createScalarData(): BiasedScalarData {
        return BiasedScalarData()
    }

    override val randomValue: Double
        get() {
            val rand = Random()
            return if (rand.nextBoolean()) upperBound else lowerBound
        }

    override val name: String
        get() = "Binary"

    fun setCeiling(ceiling: Double) {
        this.upperBound = ceiling
    }

    fun setFloor(floor: Double) {
        this.lowerBound = floor
    }

    override fun contextualIncrement(n: Neuron) {
        n.forceSetActivation(upperBound)
    }

    override fun contextualDecrement(n: Neuron) {
        n.forceSetActivation(lowerBound)
    }

    override val graphicalLowerBound: Double
        get() = lowerBound - 1

    override val graphicalUpperBound: Double
        get() = upperBound + 1

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    companion object {
        private const val DEFAULT_CEILING = 1.0

        private const val DEFAULT_FLOOR = -1.0
    }
}