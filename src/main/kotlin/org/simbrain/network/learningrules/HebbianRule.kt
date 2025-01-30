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
