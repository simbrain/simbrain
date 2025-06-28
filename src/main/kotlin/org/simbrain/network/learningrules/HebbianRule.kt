package org.simbrain.network.learningrules

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.dialogs.NetworkPreferences.defaultLearningRate
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * Standard Hebbian learning rule.
 */
class HebbianRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {

    @UserParameter(label = "Learning rate", description = "Learning rate for Hebb rule", increment = .1, minimumValue = 0.0, order = 10)
    var learningRate: Double = defaultLearningRate

    @UserParameter(
        label = "Forgetting rate",
        description = "The percent of strength to remove at each time step.",
        increment = .1,
        minimumValue = 0.0,
        maximumValue = 1.0,
        order = 20)
    var forgettingRate: Double = 0.0

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Hebbian"

    override fun copy(): SynapseUpdateRule<*, *> {
        val h = HebbianRule()
        h.learningRate = learningRate
        h.forgettingRate = forgettingRate
        return h
    }

    context(Network)
    override fun apply(connector: Connector, data: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weights
            val input = connector.source.activations
            val output = connector.target.activations
            if (forgettingRate == 0.0) {
                // delta = rate * (input * output^T)
                wm.add(output.mt(input).mul(learningRate))
            } else {
                connector.weights.mul(1 - forgettingRate).add(output.mt(input).mul(learningRate))
            }
        }
    }

    fun applyForgetting(connector: WeightMatrix) {
        connector.weights.mul(1 - forgettingRate)
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation
        if (forgettingRate == 0.0) {
            synapse.strength += (learningRate * input * output)
        } else {
            synapse.strength = (1 - forgettingRate) * synapse.strength + (learningRate * input * output)
        }
    }
}
