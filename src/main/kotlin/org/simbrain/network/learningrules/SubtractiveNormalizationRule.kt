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

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
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

    override fun deepCopy(): SynapseUpdateRule<*, *> {
        val sns = SubtractiveNormalizationRule()
        sns.learningRate = learningRate
        return sns
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation
        val averageInput = synapse.target.averageInput
        val strength = synapse.strength + ((learningRate * output * input) - (learningRate * output * averageInput))
        synapse.strength = synapse.clip(strength)
    }
}
