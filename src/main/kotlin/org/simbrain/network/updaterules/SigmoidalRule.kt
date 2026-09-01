package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.addi

/**
 * Discrete sigmoidal provides various implementations of a standard sigmoidal neuron.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class SigmoidalRule : AbstractSigmoidalRule<EmptyScalarData, EmptyMatrixData>() {

    override val timeType: Network.TimeType = Network.TimeType.DISCRETE

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        var weightedInput = neuron.input
        if (addNoise) {
            weightedInput += noiseGenerator.sampleDouble()
        }
        neuron.activation = type.valueOf(weightedInput, upperBound, lowerBound, slope)
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        val array = layer as NeuronArray
        val weightedInputs = array.inputs.clone()
        if (addNoise) {
            weightedInputs.addi(noiseGenerator.sampleDouble(array.size))
        }
        array.activations = type.valueOf(weightedInputs, upperBound, lowerBound, slope)
    }

    override fun copy(): SigmoidalRule {
        var sr = SigmoidalRule()
        sr = super.copy(sr) as SigmoidalRule
        return sr
    }

    override fun getDerivative(input: Double): Double {
        return type.derivVal(input, upperBound, lowerBound, upperBound - lowerBound)
    }

    override val name: String
        get() = "Sigmoidal (Discrete)"

}