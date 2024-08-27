package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import java.util.*

/**
 * BinaryNeuron takes one of two values based on a threshold.
 */
class BinaryRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>, BoundedUpdateRule {

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
    override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        for (i in 0 until layer.activations.nrow()) {
            layer.activations[i, 0] = binaryRule(layer.inputs[i, 0])
        }
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        neuron.activation = binaryRule(neuron.input)
    }

    fun binaryRule(inputVal: Double): Double {
        return if (inputVal > threshold) {
            upperBound
        } else {
            lowerBound
        }
    }

    override fun createMatrixData(size: Int): EmptyMatrixData {
        return EmptyMatrixData
    }

    override fun createScalarData(): EmptyScalarData {
        return EmptyScalarData
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