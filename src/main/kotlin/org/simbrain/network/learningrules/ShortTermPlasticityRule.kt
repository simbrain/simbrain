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
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **ShortTermPlasticitySynapse**.
 */
class ShortTermPlasticityRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    /**
     * Plasticity type.
     */
    @UserParameter(label = "Plasticity Type", description = "Plasticity Type", increment = 1.0, order = 1)
    var plasticityType: Int = STD

    /**
     * Pseudo spike threshold.
     */
    @UserParameter(label = "Spike Threshold", description = "Pseudo Spike Threshold", increment = .1, order = 2)
    var firingThreshold: Double = DEFAULT_FIRING_THRESHOLD

    /**
     * Base line strength.
     */
    @UserParameter(label = "Line Strength", description = "Base line strength", increment = .1, order = 3)
    var baseLineStrength: Double = DEFAULT_BASE_LINE_STRENGTH

    /**
     * Input threshold.
     */
    @UserParameter(label = "Input Threshold", description = "Input threshold", increment = .1, order = 4)
    var inputThreshold: Double = DEFAULT_INPUT_THRESHOLD

    /**
     * Bump rate.
     */
    @UserParameter(label = "Bump rate", description = "Bump Rate", increment = .1, order = 5)
    var bumpRate: Double = DEFAULT_BUMP_RATE

    /**
     * Rate at which the synapse will decay.
     */
    @UserParameter(
        label = "Decay Rate",
        description = "Rate at which the synapse will decay",
        increment = .1,
        order = 6
    )
    var decayRate: Double = DEFAULT_DECAY_RATE

    /**
     * Activated.
     */
    @UserParameter(label = "Activated", description = "Activated", increment = .1, order = 7)
    private var activated = DEFAULT_ACTIVATED

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Short Term Plasticity"

    override fun deepCopy(): SynapseUpdateRule<*, *> {
        val stp = ShortTermPlasticityRule()
        stp.baseLineStrength = baseLineStrength
        stp.bumpRate = bumpRate
        stp.decayRate = decayRate
        stp.inputThreshold = inputThreshold
        stp.plasticityType = plasticityType
        return stp
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        // Determine whether to activate short term dynamics

        activated = if (synapse.source.updateRule is SpikingNeuronUpdateRule<*, *>) {
            if (synapse.source.isSpike) {
                true
            } else {
                false
            }
        } else {
            if (synapse.source.activation > firingThreshold) {
                true
            } else {
                false
            }
        }
        var strength = synapse.strength
        if (activated) {
            if (plasticityType == STD) {
                strength -= (bumpRate * (strength - synapse.lowerBound))
            } else {
                strength += (bumpRate * (synapse.upperBound - strength))
            }
        } else {
            strength -= (decayRate * (strength - baseLineStrength))
        }

        synapse.strength = synapse.clip(strength)
    }

    companion object {
        // TODO: Enum
        /**
         * STD.
         */
        private const val STD = 0

        /**
         * Plasticity type.
         */
        const val DEFAULT_PLASTICITY_TYPE: Int = STD

        /**
         * Pseudo spike threshold.
         */
        const val DEFAULT_FIRING_THRESHOLD: Double = 0.0

        /**
         * Base line strength.
         */
        const val DEFAULT_BASE_LINE_STRENGTH: Double = 1.0

        /**
         * Input threshold.
         */
        const val DEFAULT_INPUT_THRESHOLD: Double = 0.0

        /**
         * Bump rate.
         */
        const val DEFAULT_BUMP_RATE: Double = .5

        /**
         * Rate at which the synapse will decay.
         */
        const val DEFAULT_DECAY_RATE: Double = .2

        /**
         * Activated.
         */
        const val DEFAULT_ACTIVATED: Boolean = false
    }
}
