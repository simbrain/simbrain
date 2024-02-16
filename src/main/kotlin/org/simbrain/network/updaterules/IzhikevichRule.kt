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
package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * **IzhikevichNeuron**. Default values correspond to "tonic spiking". TODO:
 * Store a bunch of useful parameters, and add a combo box to switch between the
 * different types. Students could just look it up, but this would be
 * faster/cooler. Just a thought.
 */
class IzhikevichRule : SpikingNeuronUpdateRule<SpikingScalarData, SpikingMatrixData>(), NoisyUpdateRule {

    private var recovery = 0.0

    @UserParameter(
        label = "A",
        description = "Parameter for recovery variable.",
        increment = .01,
        order = 1,
        probDist = "Uniform",
        probParam1 = .01,
        probParam2 = .12
    )
    var a: Double = 0.0

    @UserParameter(
        label = "B",
        description = "Parameter for recovery variable.",
        increment = .01,
        order = 2,
        probDist = "Uniform",
        probParam1 = .15,
        probParam2 = .3
    )
    var b: Double = 0.0

    @UserParameter(
        label = "C",
        description = "The value for v which occurs after a spike.",
        increment = .01,
        order = 3,
        probDist = "Uniform",
        probParam1 = -70.0,
        probParam2 = -45.0
    )
    var c: Double = 0.0

    @UserParameter(
        label = "D",
        description = "A constant value added to u after spikes.",
        increment = .01,
        order = 4,
        probDist = "Uniform",
        probParam1 = 0.02,
        probParam2 = 10.0
    )
    var d: Double = 0.0

    /**
     * Constant background current.
     */
    @UserParameter(label = "I bkgd", description = "Constant background current.", increment = .1, order = 5)
    private var iBg = 14.0

    @UserParameter(label = "Threshold", description = "Threshold value to signal a spike", increment = .1, order = 10)
    var threshold = 30.0

    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    override var addNoise = false

    override fun copy(): IzhikevichRule {
        val ir = IzhikevichRule()
        ir.a = a
        ir.b = b
        ir.c = c
        ir.d = d
        ir.setiBg(getiBg())
        ir.addNoise = addNoise
        ir.noiseGenerator = noiseGenerator.copy()
        return ir
    }

    context(Network)
    override fun apply(neuron: Neuron, data: SpikingScalarData) {
        val activation = neuron.activation
        var inputs = neuron.input
        if (addNoise) {
            inputs += noiseGenerator.sampleDouble()
        }
        inputs += iBg
        recovery += timeStep * (a * (b * activation - recovery))
        var value = activation + timeStep * (.04 * (activation * activation) + 5 * activation + 140 - recovery +
                inputs)
        if (value >= threshold) {
            value = c
            recovery += d
            neuron.isSpike = true
        } else {
            neuron.isSpike = false
        }
        neuron.activation = value
    }

    // Equal chance of spiking or not spiking, taking on any value between
    // the resting potential and the threshold if not.
    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double = 2 * (threshold - c) * Math.random() + c

    fun getiBg(): Double {
        return iBg
    }

    fun setiBg(iBg: Double) {
        this.iBg = iBg
    }

    override val name: String
        get() = "Izhikevich"

    override val graphicalUpperBound: Double
        get() = threshold

    override val graphicalLowerBound: Double
        get() = c

}


