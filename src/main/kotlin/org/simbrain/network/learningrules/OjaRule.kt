/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.NetworkPreferences.defaultLearningRate
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.broadcastMultiply

/**
 * **OjaSynapse** is a synapse which asymptotically normalizes the sum of
 * squares of the weights attaching to a neuron to a user-defined value.
 */
class OjaRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    /**
     * Learning rate.
     */
    @UserParameter(label = "Learning rate", description = "Learning rate for Oja rule", increment = .1, minimumValue = 0.0, order = 1)
    var learningRate = defaultLearningRate

    // TODO: check description
    /**
     * Normalization factor.
     */
    @UserParameter(label = "Normalize to", description = "Normalization factor for Oja rule", increment = .1, order = 1)
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
        val strength = synapse.strength + learningRate * (input * output - (output * output * synapse.strength
                / normalizationFactor))
        synapse.strength = synapse.clip(strength)
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weightMatrix
            val input = (connector.source as NeuronArray).activations
            val output = (connector.target as NeuronArray).activations
            // delta    = rate * (input * output^T - input "broadcast multiplied by" weight matrix)
            //          = rate * (hebbTerm - weightDecayTerm)
            val hebbTerm = output.mt(input)
            val weightDecayTerm = wm.broadcastMultiply(output)
            wm.add(hebbTerm.sub(weightDecayTerm).mul(learningRate))
        }
    }
}