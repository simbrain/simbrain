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

/**
 * **Hebbian** implements a standard Hebbian learning rule.
 */
class HebbianRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    @UserParameter(label = "Learning rate", description = "Learning rate for Hebb rule", increment = .1, order = 1)
    var learningRate: Double = defaultLearningRate

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Hebbian"

    override fun deepCopy(): SynapseUpdateRule<*, *> {
        val h = HebbianRule()
        h.learningRate = learningRate
        return h
    }

    context(Network)
    override fun apply(connector: Connector, data: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weightMatrix
            val input = (connector.source as NeuronArray).activations
            val output = (connector.target as NeuronArray).activations
            // delta = rate * (input * output^T)
            wm.add(output.mt(input).mul(learningRate))
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation
        val strength = synapse.clip(synapse.strength + (learningRate * input * output))
        synapse.strength = strength
    }
}
