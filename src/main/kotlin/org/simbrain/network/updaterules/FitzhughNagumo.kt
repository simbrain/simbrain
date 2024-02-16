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

import org.simbrain.network.core.*
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath.clip
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.Producible

/**
 * An early 2d spiking model that models the action potential. At rest with no inputs, goes to the values shown in
 * the phase-portrait on the scholarpedia page.
 *
 * @see http://www.scholarpedia.org/article/FitzHugh-Nagumo_model
 */
class FitzhughNagumo : SpikingNeuronUpdateRule<FitzHughData, FitzHughMatrixData>(), NoisyUpdateRule {

    /**
     * Constant background current. KEEP
     */
    @UserParameter(
        label = "Background Current (nA)",
        description = "Background current to the cell.",
        increment = .1,
        order = 4
    )
    private var iBg = 0.0

    /**
     * Threshold value to signal a spike. KEEP
     */
    @UserParameter(
        label = "Spike threshold",
        description = "Threshold value to signal a spike.",
        increment = .1,
        order = 5
    )
    var threshold = 1.9

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to the neuron.
     */
    override var addNoise = false

    /**
     * Recovery rate
     */
    @UserParameter(
        label = "A (Recovery Rate)",
        description = "Abstract measure of how much \"resource\" a cell is depleting in response to large changes in voltage.",
        increment = .1,
        order = 1
    )
    var a = 0.08

    /**
     * Recovery dependence on voltage.
     */
    @UserParameter(
        label = "B (Rec. Voltage Dependence)",
        description = "How much the recovery variable w depends on voltage.",
        increment = .1,
        order = 2
    )
    var b = 1.0

    /**
     * Recovery self-dependence.
     */
    @UserParameter(
        label = "C (Rec. Self Dependence)",
        description = "How quickly the recovery variable recovers to its baseline value.",
        increment = .1,
        order = 3
    )
    var c = 0.8
    override fun copy(): FitzhughNagumo {
        val copy = FitzhughNagumo()
        copy.a = a
        copy.b = b
        copy.c = c
        copy.threshold = threshold
        copy.addNoise = addNoise
        copy.noiseGenerator = noiseGenerator.copy()
        return copy
    }

    context(Network)
    override fun apply(neuron: Neuron, data: FitzHughData) {
        val (spiked, v, w) = fitzhughNagumoRule(neuron.activation, data.w, neuron.input, timeStep)
        neuron.isSpike = spiked
        neuron.activation = v
        data.w = w
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: FitzHughMatrixData) {
        if (layer is NeuronArray) {
            for (i in 0 until layer.size()) {
                val (spiked, v, w) = fitzhughNagumoRule(
                    layer.activations.get(i, 0),
                    dataHolder.w[i],
                    layer.inputs.get(i, 0),
                    timeStep
                )
                dataHolder.setHasSpiked(i, spiked, time)
                layer.activations.set(i, 0, v)
                dataHolder.w[i] = w
            }
        }
    }

    private fun fitzhughNagumoRule(
        initV: Double,
        initW: Double,
        externalInput: Double,
        timeStep: Double
    ): Triple<Boolean, Double, Double> {
        var inputs = externalInput
        var v = initV
        var w = initW
        if (addNoise) {
            inputs += noiseGenerator.sampleDouble()
        }
        inputs += iBg
        w += timeStep * (a * (b * v + 0.7 - c * w))
        v += timeStep * (v - v * v * v / 3 - w + inputs)

        v = clip(v, -1000.0, 1000.0)

        if (v >= threshold) {
            return Triple(true, v, w)
        } else {
            return Triple(false, v, w)
        }
    }

    override fun createScalarData(): FitzHughData {
        return FitzHughData()
    }

    override fun createMatrixData(size: Int): FitzHughMatrixData {
        return FitzHughMatrixData(size)
    }

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double = 2 * (threshold - c) * Math.random() + c

    fun getiBg(): Double {
        return iBg
    }

    fun setiBg(iBg: Double) {
        this.iBg = iBg
    }

    override val name: String
        get() = "FitzhughNagumo"
}


class FitzHughData(
    @UserParameter(
        label = "w", description = "Recovery variables."
    )
    @get:Producible
    var w: Double = 0.0,
) : SpikingScalarData() {
    override fun copy(): FitzHughData {
        return FitzHughData(w)
    }
}

class FitzHughMatrixData(size: Int) : SpikingMatrixData(size) {
    @get:Producible
    var w = DoubleArray(size)
    override fun copy() = FitzHughMatrixData(size).also {
        commonCopy(it)
        it.w = w.copyOf()
    }
}
