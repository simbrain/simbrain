package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.toDoubleArray
import kotlin.math.exp

class SoftmaxRule: NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>() {


    @UserParameter(
        label = "Temperature",
        description = """1 is default. 0 to 1 is a flatter distribution. Above 1 is a sharper distribution.""",
        minimumValue = 0.0,
        increment = .1,
        order = 10)
    var temperature = 1.0

    context(Network) override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        val exponentials = layer.inputs.toDoubleArray().map { exp(it/temperature) }
        val total = exponentials.sum()
        layer.setActivations(exponentials.map { it/total }.toDoubleArray())
    }

    context(Network) override fun apply(neuron: Neuron, data: EmptyScalarData) {
        throw UnsupportedOperationException("SoftmaxRule does not support scalar data")
    }

    override val name = "Softmax"
    override val timeType = Network.TimeType.DISCRETE

    override fun copy() = SoftmaxRule().also {
        it.temperature = temperature
    }

}