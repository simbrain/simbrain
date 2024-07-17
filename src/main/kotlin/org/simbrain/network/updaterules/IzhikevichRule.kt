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
import org.simbrain.util.addi
import org.simbrain.util.copyFrom
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix

/**
 * **IzhikevichNeuron**. Default values correspond to "tonic spiking". TODO:
 * Store a bunch of useful parameters, and add a combo box to switch between the
 * different types. Students could just look it up, but this would be
 * faster/cooler. Just a thought.
 */
class IzhikevichRule : SpikingNeuronUpdateRule<IzhikevichScalarData, IzhikevichMatrixData>(), NoisyUpdateRule {

    @UserParameter(
        label = "A",
        description = "Parameter for recovery variable.",
        increment = .01,
        order = 1,
        probDist = "Uniform",
        probParam1 = .01,
        probParam2 = .12
    )
    var a: Double = 0.02

    @UserParameter(
        label = "B",
        description = "Parameter for recovery variable.",
        increment = .01,
        order = 2,
        probDist = "Uniform",
        probParam1 = .15,
        probParam2 = .3
    )
    var b: Double = 0.2

    @UserParameter(
        label = "C",
        description = "The value for v which occurs after a spike.",
        increment = .01,
        order = 3,
        probDist = "Uniform",
        probParam1 = -70.0,
        probParam2 = -45.0
    )
    var c: Double = -65.0

    @UserParameter(
        label = "D",
        description = "A constant value added to u after spikes.",
        increment = .01,
        order = 4,
        probDist = "Uniform",
        probParam1 = 0.02,
        probParam2 = 10.0
    )
    var d: Double = 6.0

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

    override fun createScalarData(): IzhikevichScalarData {
        return IzhikevichScalarData()
    }

    override fun createMatrixData(size: Int): IzhikevichMatrixData {
        return IzhikevichMatrixData(size)
    }

    context(Network)
    override fun apply(neuron: Neuron, data: IzhikevichScalarData) {
        val activation = neuron.activation
        var inputs = neuron.input
        if (addNoise) {
            inputs += noiseGenerator.sampleDouble()
        }
        inputs += iBg
        val (newActivation, spiked, newRecovery) = izhikevichRule(timeStep, inputs, activation, data.recovery)
        neuron.activation = newActivation
        neuron.isSpike = spiked
        data.recovery = newRecovery
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: IzhikevichMatrixData) {
        if (layer is NeuronArray) {
            val inputs = layer.inputs

            if (addNoise) {
                inputs.addi(noiseGenerator.sampleDouble(layer.size()))
            }

            inputs.add(iBg)

            val result = buildList {
                for (i in 0 until layer.size()) {
                    val activation = layer.activations.get(i, 0)
                    val input = layer.inputs.get(i, 0)
                    add(izhikevichRule(timeStep, input, activation, dataHolder.recoveryMatrix[i, 0]))
                }
            }

            val activations = Matrix.column(result.map { it.activation }.toDoubleArray())
            val spikes = result.map { it.isSpiked }.toBooleanArray()
            val recovery = Matrix.column(result.map { it.recovery }.toDoubleArray())

            layer.activations = activations
            spikes.copyInto(dataHolder.spikes)
            dataHolder.recoveryMatrix.copyFrom(recovery)
        }
    }

    private fun izhikevichRule(
        timeStep: Double,
        input: Double,
        activation: Double,
        recovery: Double
    ): IzhikevichState {
        var newRecovery = recovery + timeStep * (a * (b * activation - recovery))
        var value = activation + timeStep * (.04 * (activation * activation) + 5 * activation + 140 - recovery + input)
        if (value >= threshold) {
            value = c
            newRecovery += d
            return IzhikevichState(value, true, newRecovery)
        }
        return IzhikevichState(value, false, newRecovery)
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

data class IzhikevichState(val activation: Double, val isSpiked: Boolean, val recovery: Double)

class IzhikevichScalarData(
    @UserParameter(label = "Recovery", increment = .01, order = 1)
    var recovery: Double = 0.0
) : SpikingScalarData() {
    override fun copy(): SpikingScalarData {
        val copy = IzhikevichScalarData()
        copy.recovery = recovery
        return copy
    }
}

class IzhikevichMatrixData(size: Int) : SpikingMatrixData(size) {
    @UserParameter(label = "Recovery Matrix", description = "Recovery matrix for each neuron")
    var recoveryMatrix = Matrix(size, 1)
    override fun copy(): SpikingMatrixData {
        return IzhikevichMatrixData(size).also {
            commonCopy(it)
            it.recoveryMatrix.copyFrom(recoveryMatrix)
        }
    }
}
