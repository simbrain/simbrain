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
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.sin

/**
 * **SinusoidalNeuron** produces a sine wave.
 */
class SinusoidalRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), ActivityGenerator, ClippedUpdateRule, NoisyUpdateRule {
    /**
     * Phase.
     */
    @UserParameter(
        label = "Phase",
        description = "The phase tells us where we start in a period of the sinusoidal oscillation.",
        order = 1
    )
    var phase: Double = 1.0

    /**
     * Frequency.
     */
    @UserParameter(
        label = "Frequency",
        description = "The frequency tells us how frequently the activation oscillates.",
        order = 2
    )
    var frequency: Double = .1

    /**
     * The upper boundary of the activation.
     */
    override var upperBound: Double = 1.0

    /**
     * The lower boundary of the activation.
     */
    override var lowerBound: Double = -1.0

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to the neuron.
     */
    override var addNoise: Boolean = false

    /**
     * Bounded update rule is automatically clippable.  It is not needed here since sigmoids automatically respect
     * upper and lower bounds but can still be turned on to constrain contextual increment and decrement.
     */
    override var isClipped: Boolean = false

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): SinusoidalRule {
        val sn = SinusoidalRule()
        sn.phase = phase
        sn.frequency = frequency
        sn.addNoise = addNoise
        sn.noiseGenerator = noiseGenerator.copy()
        return sn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        val upperBound = upperBound
        val lowerBound = lowerBound
        val range = upperBound - lowerBound
        var `val` =
            ((range / 2) * sin(frequency * time + phase)) + ((upperBound + lowerBound) / 2)

        if (addNoise) {
            `val` += noiseGenerator.sampleDouble()
        }

        neuron.activation = `val`
    }

    override val name: String
        get() = "Sinusoidal"

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double {
        val rand = (2 * Math.PI) * Math.random()
        val range = upperBound - lowerBound
        return ((range / 2) * sin(frequency * rand + phase)) + ((upperBound + lowerBound) / 2)
    }
}
