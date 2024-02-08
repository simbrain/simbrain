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
import kotlin.math.pow

/**
 * **HebbianCPCA**. TODO: No Doc.
 */
class HebbianCPCARule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    /**
     * Learning rate.
     */
    @UserParameter(
        label = "Learning rate",
        description = "Learning rate for Hebb CPCA",
        minimumValue = 0.0,
        maximumValue = DEFAULT_M,
        increment = .1,
        order = 1
    )
    var learningRate: Double = 0.0

    /**
     * Max Weight Value.
     */
    @UserParameter(
        label = "m",
        description = "Max Weight",
        minimumValue = -10.0,
        maximumValue = 10.0,
        increment = .1,
        order = 1
    )
    var m: Double = 0.0

    /**
     * Weight offset.
     */
    @UserParameter(
        label = "Theta",
        description = "Weight Offset value",
        minimumValue = -10.0,
        maximumValue = 10.0,
        increment = .1,
        order = 1
    )
    var theta: Double = 0.0

    /**
     * Lambda.
     */
    @UserParameter(
        label = "Lambda",
        description = "Sigmomid Function",
        minimumValue = -1.0,
        maximumValue = 10.0,
        increment = .1,
        order = 1
    )
    var lambda: Double = 0.0

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Hebbian CPCA"

    override fun deepCopy(): SynapseUpdateRule<*, *> {
        val learningRule = HebbianCPCARule()
        learningRule.learningRate = learningRate
        learningRule.m = m
        learningRule.theta = theta
        learningRule.lambda = lambda
        return learningRule
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        // Updates the synapse (see equation 4.18 in O'Reilly and Munakata).

        val input = synapse.source.activation
        val output = synapse.target.activation

        val deltaW = learningRate * ((output * input) - (output * synapse.strength)) // Equation
        // 4.12
        // deltaW = learningRate * (output * input * (m - strength) + output *
        // (1 - input) * (-strength));
        // strength = sigmoidal(strength);
        // strength = clip(strength + deltaW);
        synapse.strength = synapse.strength + deltaW
    }

    /**
     * Sigmoidal Function (see equation 4.23 in O'Reilly and Munakata).
     *
     * @param arg value to send to sigmoidal
     * @return value of sigmoidal
     */
    private fun sigmoidal(arg: Double): Double {
        return 1 / (1 + (theta * (arg / (1 - arg))).pow(-lambda))
    }

    companion object {
        /**
         * Default Maximum weight value (see equation 4.19 in O'Reilly and
         * Munakata).
         */
        const val DEFAULT_M: Double = .5 / .15
    }
}
