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
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **LogisticNeuron** updates using the logistic equation, which is chaotic
 * for the default growth rate. Does not use inputs from other neurons.
 */
class LogisticRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>, ActivityGenerator, ClippedUpdateRule {
    /**
     * Growth rate.
     */
    @UserParameter(
        label = "Growth Rate", description = "A number that determines the exact form of the quadratic function. "
                + "It must be between 0 and 4.", minimumValue = 0.0, maximumValue = 4.0, order = 1
    )
    var growthRate: Double = 3.9

    override var upperBound: Double = 10.0

    override var lowerBound: Double = -10.0

    constructor() : super()

    constructor(lr: LogisticRule, n: Neuron?) : super() {
        this.upperBound = lr.upperBound
        this.lowerBound = lr.lowerBound
        this.growthRate = lr.growthRate
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): LogisticRule {
        val ln = LogisticRule()
        ln.growthRate = growthRate
        ln.upperBound = upperBound
        ln.lowerBound = lowerBound
        return ln
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        // TODO: Note that the inputs have to be within the neuron's bounds for
        // behavior to be reasonable.

        var x = neuron.activation

        var y = (x - lowerBound) / (upperBound - lowerBound)
        y = growthRate * y * (1 - y)
        x = ((upperBound - lowerBound) * y) + lowerBound

        neuron.activation = clip(x)
    }

    override val name: String
        get() = "Logistic"

    override var isClipped: Boolean
        get() = true
        set(clipping) {
        }
}