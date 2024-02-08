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
package org.simbrain.network.updaterules.activity_generators

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.ActivityGenerator
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter

/**
 * **StochasticNeuron** is a simple type of random neuron which takes the
 * value of the upper bound if a random variable is above a specified firing
 * probability, and the lower bound otherwise. Ignores inputs.
 *
 *
 * TODO: Separate spiking from non-spiking version?
 */
class StochasticRule : SpikingNeuronUpdateRule<SpikingScalarData, SpikingMatrixData>(), ActivityGenerator {
    /**
     * Probability the neuron will fire.
     */
    @UserParameter(
        label = "Firing Probability",
        description = "This parameter determines the probability that the generator will fire, "
                + "causing it to have an activation equal to its upper bound, given an iteration.",
        order = 1
    )
    var firingProbability: Double = DEFAULT_FIRING_PROBABILITY

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun deepCopy(): StochasticRule {
        val sn = StochasticRule()
        sn.firingProbability = firingProbability
        return sn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: SpikingScalarData) {
        val rand = Math.random()
        if (rand > 1 - firingProbability) {
            neuron.isSpike = true
            neuron.activation = 1.0
        } else {
            neuron.isSpike = false
            neuron.activation = 0.0 // Make this a separate variable?
        }
    }

    override val name: String
        get() = "Stochastic"

    companion object {
        /**
         * The default firing probability for the Neuron.
         */
        private const val DEFAULT_FIRING_PROBABILITY = .05
    }
}
