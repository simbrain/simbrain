package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **HebbianThresholdSynapse**.
 */
class HebbianThresholdRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {

    @UserParameter(
        label = "Learning rate",
        description = "Learning rate for Hebb threshold rule",
        increment = .1,
        order = 1
    )
    var learningRate: Double = 0.1

    @UserParameter(
        label = "Threshold",
        description = "Output threshold for Hebb threshold rule",
        increment = .1,
        order = 1
    )
    var outputThreshold: Double = .5

    @UserParameter(
        label = "Threshold Momentum",
        description = "Output threshold momentum for Hebb threshold rule",
        increment = .1,
        order = 1
    )
    var outputThresholdMomentum: Double = .1

    @UserParameter(
        label = "Sliding Threshold",
        description = "Use sliding output threshold for Hebb threshold rule",
        order = 1
    )
    var useSlidingOutputThreshold: Boolean = false

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Hebbian Threshold"

    override fun copy(): HebbianThresholdRule {
        val h = HebbianThresholdRule()
        h.learningRate = learningRate
        h.outputThreshold = outputThreshold
        h.outputThresholdMomentum = outputThresholdMomentum
        h.useSlidingOutputThreshold = useSlidingOutputThreshold
        return h
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation

        if (useSlidingOutputThreshold) {
            outputThreshold += (outputThresholdMomentum * ((output * output) - outputThreshold))
        }
        synapse.strength += learningRate * input * output * (output - outputThreshold)
    }

    // Note: This function has not been tested.
    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weights
            val input = (connector.source as NeuronArray).activations
            val output = (connector.target as NeuronArray).activations
            
            // Matrix-based Hebbian Threshold: vectorized version
            // Note: For simplicity, we use a global threshold for all neurons in matrix version
            // In a more sophisticated implementation, we might maintain per-neuron thresholds
            
            if (useSlidingOutputThreshold) {
                // Update threshold based on average output squared
                var sum = 0.0
                for (i in 0 until output.nrow()) {
                    val value = output[i, 0]
                    sum += value * value
                }
                val avgOutputSquared = sum / output.nrow()
                outputThreshold += (outputThresholdMomentum * (avgOutputSquared - outputThreshold))
            }
            
            // For each target neuron (row)
            for (i in 0 until wm.nrow()) {
                val outputActivation = output[i, 0]
                for (j in 0 until wm.ncol()) {
                    val inputActivation = input[j, 0]
                    wm[i, j] += learningRate * inputActivation * outputActivation * (outputActivation - outputThreshold)
                }
            }
        }
    }
}
