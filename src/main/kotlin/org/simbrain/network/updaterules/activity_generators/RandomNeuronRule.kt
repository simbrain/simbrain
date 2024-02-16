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
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.ActivityGenerator
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * **RandomNeuron** produces random activations within specified parameters.
 */
class RandomNeuronRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>, ActivityGenerator, ClippedUpdateRule, NoisyUpdateRule {
    /**
     * Noise source.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    override var upperBound: Double = 1.0

    override var lowerBound: Double = -1.0

    /**
     * Bounded update rule is automatically clippable.  It is not needed here since sigmoids automatically respect
     * upper and lower bounds but can still be turned on to constrain contextual increment and decrement.
     */
    override var isClipped: Boolean = false

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    constructor() : super()

    constructor(rn: RandomNeuronRule, n: Neuron?) : super() {
        noiseGenerator = rn.noiseGenerator.copy()
    }

    override fun copy(): RandomNeuronRule {
        val rn = RandomNeuronRule()
        rn.noiseGenerator = noiseGenerator.copy()
        return rn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        neuron.activation = noiseGenerator.sampleDouble()
    }

    override val name: String
        get() = "Random"

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double = noiseGenerator.sampleDouble()

    override var addNoise: Boolean
        get() = true
        set(noise) {
        }
}
