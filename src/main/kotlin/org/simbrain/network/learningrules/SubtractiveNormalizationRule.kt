package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **SubtractiveNormalizationSynapse**.
 */
class SubtractiveNormalizationRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    // TODO: check description
    /**
     * Momentum.
     */
    @UserParameter(label = "Learning rate", description = "Momentum", increment = .1, order = 1)
    var learningRate: Double = 0.0

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Subtractive Normalization"

    override fun copy(): SynapseUpdateRule<*, *> {
        val sns = SubtractiveNormalizationRule()
        sns.learningRate = learningRate
        return sns
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation
        val averageInput = synapse.target.averageInput
        synapse.strength += (learningRate * output * input) - (learningRate * output * averageInput)
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weights
            val input = (connector.source as NeuronArray).activations
            val output = (connector.target as NeuronArray).activations
            
            // Matrix-based Subtractive Normalization: simplified version
            // Note: In matrix form, we compute a global average input rather than per-neuron averageInput
            // since individual neuron properties are not accessible in matrix operations
            
            var totalInput = 0.0
            for (i in 0 until input.nrow()) {
                totalInput += input[i, 0]
            }
            val globalAverageInput = totalInput / input.nrow()
            
            for (i in 0 until wm.nrow()) {
                val outputActivation = output[i, 0]
                
                for (j in 0 until wm.ncol()) {
                    val inputActivation = input[j, 0]
                    wm[i, j] += (learningRate * outputActivation * inputActivation) - (learningRate * outputActivation * globalAverageInput)
                }
            }
        }
    }
}
