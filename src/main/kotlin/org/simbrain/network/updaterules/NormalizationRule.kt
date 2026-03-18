package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.toDoubleArray

/**
 * Normalization rule for [NeuronArray]. Divides all activations by their sum so they sum to 1.
 * If the sum is zero, sets all activations to a uniform value (1/n).
 *
 * Not defined for scalar neurons.
 */
class NormalizationRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), BoundedUpdateRule {

    context(Network) override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        if (layer !is NeuronArray) return

        val activations = layer.inputs.toDoubleArray()
        val total = activations.sum()
        val result = if (total != 0.0) {
            activations.map { it / total }.toDoubleArray()
        } else {
            DoubleArray(activations.size) { 1.0 / activations.size }
        }
        layer.setActivations(result)
    }

    context(Network) override fun apply(neuron: Neuron, data: EmptyScalarData) {
        throw UnsupportedOperationException("NormalizationRule does not support scalar data")
    }

    override val name = "Normalization"
    override val timeType = Network.TimeType.DISCRETE

    override fun createMatrixData(size: Int) = EmptyMatrixData

    override fun copy() = NormalizationRule()

    override var upperBound: Double
        get() = 1.0
        set(value) {}

    override var lowerBound: Double
        get() = 0.0
        set(value) {}
}
