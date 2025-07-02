package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.NetworkPreferences.defaultLearningRate
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.*

/**
 * **OjaSynapse** is a synapse which asymptotically normalizes the sum of
 * squares of the weights attaching to a neuron to a user-defined value.
 */
class OjaRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {

    @UserParameter(label = "Learning rate", description = "Learning rate for Oja rule", increment = .1, minimumValue = 0.0, order = 1)
    var learningRate = defaultLearningRate

    @UserParameter(label = "Normalization Factor", description = "Normalization factor for Oja rule", increment = .1, order = 1)
    var normalizationFactor = 1.0
    override fun init(synapse: Synapse) {}

    override val name: String
        get() = "Oja"

    override fun copy(): SynapseUpdateRule<*, *> {
        val os = OjaRule()
        os.normalizationFactor = normalizationFactor
        os.learningRate = learningRate
        return os
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation
        synapse.strength += learningRate * (input * output - (output * output * synapse.strength
                / normalizationFactor))
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weights
            val input = (connector.source as NeuronArray).activations
            val output = (connector.target as NeuronArray).activations
            // delta    = rate * (input * output^T - (output ⨀ output^T rowScale wm )/N)
            //          = rate * (hebbTerm - stabilizationTerm)
            val hebbTerm = output.mt(input)
            val decayCoeffs = output.hadamard(output)  // elementwise square of y
            val stabilizationTerm = wm
                .scaleRows(decayCoeffs)    // rows scaled by y_i²
                .div(normalizationFactor)
            wm.add(hebbTerm.sub(stabilizationTerm).mul(learningRate))
        }
    }
}