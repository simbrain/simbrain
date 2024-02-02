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
package org.simbrain.network.neuron_update_rules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.max

/**
 * **LinearNeuron** is a standard linear neuron.
 */
open class LinearRule : NeuronUpdateRule<BiasedScalarData, BiasedMatrixData>(), DifferentiableUpdateRule,
    NoisyUpdateRule, BoundedUpdateRule {
    override var upperBound by GuiEditable(
        initValue = DEFAULT_UPPER_BOUND,
        label = "Upper Bound",
        description = "Upper bound that determines the maximum level of activity of a node.",
        order = -20,
        onUpdate = {
            enableWidget(widgetValue(::clippingType) == ClippingType.PiecewiseLinear)
        }
    )

    override var lowerBound by GuiEditable(
        initValue = DEFAULT_LOWER_BOUND,
        label = "Lower Bound",
        description = "Lower bound that determines the minimum level of activity of a node.",
        order = -10,
        onUpdate = {
            enableWidget(
                widgetValue(::clippingType) == ClippingType.PiecewiseLinear ||
                        widgetValue(::clippingType) == ClippingType.Relu
            )
        }
    )

    @UserParameter(
        label = "Type",
        description = "No clipping, clip floor and ceiling (piecewise linear), clip floor (relu)",
        order = 10
    )
    var clippingType = ClippingType.PiecewiseLinear

    /**
     * Note that Relu case ignores provided bounds, though those bounds are still used by contextual increment and
     * decrement.
     */
    enum class ClippingType {
        NoClipping {
            override fun toString(): String {
                return "No clipping"
            }
        },
        PiecewiseLinear {
            override fun toString(): String {
                return "Piecewise Linear"
            }
        },
        Relu {
            override fun toString(): String {
                return "Relu"
            }
        }
    }

    @UserParameter(label = "Slope", description = "Slope of linear rule", increment = .1, order = 20)
    var slope = 1.0
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to the neuron.
     */
    override var addNoise = false
    override fun apply(neuron: Neuron, data: BiasedScalarData) {
        neuron.activation = linearRule(neuron.input, data.bias)
    }



    override fun apply(array: Layer, data: BiasedMatrixData) {
        for (i in 0 until array!!.outputs.nrow()) {
            array.outputs[i, 0] = linearRule(array.inputs[i, 0], data.biases[i, 0])
        }
    }

    fun linearRule(input: Double, bias: Double): Double {
        var ret = input * slope + bias
        if (addNoise) {
            ret += noiseGenerator.sampleDouble()
        }
        return when (clippingType) {
            ClippingType.NoClipping -> ret
            ClippingType.Relu -> max(0.0, ret)
            ClippingType.PiecewiseLinear -> SimbrainMath.clip(ret, lowerBound, upperBound)
        }
    }

    override fun createMatrixData(size: Int): BiasedMatrixData {
        return BiasedMatrixData(size)
    }

    override fun createScalarData(): BiasedScalarData {
        return BiasedScalarData()
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun deepCopy(): LinearRule {
        val ln = LinearRule()
        ln.slope = slope
        ln.clippingType = clippingType
        ln.addNoise = addNoise
        ln.upperBound = upperBound
        ln.lowerBound = lowerBound
        ln.noiseGenerator = noiseGenerator.deepCopy()
        return ln
    }

    override fun getDerivative(`val`: Double): Double {
        return when (clippingType) {
            ClippingType.NoClipping -> slope
            ClippingType.Relu -> if (`val` <= 0) 0.0 else slope
            ClippingType.PiecewiseLinear -> if (`val` <= lowerBound || `val` >= upperBound) 0.0 else slope
        }
    }


    override val name: String
        get() = "Linear"

}

/**
 * The Default upper bound.
 */
private const val DEFAULT_UPPER_BOUND = 10.0

/**
 * The Default lower bound.
 */
private const val DEFAULT_LOWER_BOUND = -10.0
