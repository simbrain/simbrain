package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import java.util.*

/**
 * BinaryNeuron takes one of two values based on a threshold.
 */
class BinaryRule : NeuronUpdateRule<BiasedScalarData, BiasedMatrixData>, BoundedUpdateRule {

    @UserParameter(label = "Threshold", description = "Threshold for binary neurons.", increment = .1, order = 1)
    var threshold: Double = .5

    override var upperBound: Double = 1.0

    override var lowerBound: Double = 0.0

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
        for (i in 0 until layer.activations.nrow()) {
            layer.activations[i, 0] = binaryRule(layer.inputs[i, 0], dataHolder.biases[i, 0])
        }
    }

    context(Network)
    override fun apply(neuron: Neuron, data: BiasedScalarData) {
        neuron.activation = binaryRule(neuron.input, data.bias)
    }

    fun binaryRule(inputVal: Double, bias: Double): Double {
        val wtdInput = inputVal + bias
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

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double {
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
        n.activation = upperBound
    }

    override fun contextualDecrement(n: Neuron) {
        n.activation = lowerBound
    }

    override val graphicalLowerBound: Double
        get() = lowerBound - 1

    override val graphicalUpperBound: Double
        get() = upperBound + 1

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

}