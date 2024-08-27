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

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.addi

/**
 * Discrete sigmoidal provides various implementations of a standard sigmoidal neuron.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class SigmoidalRule : AbstractSigmoidalRule() {

    override val timeType: Network.TimeType = Network.TimeType.DISCRETE

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        var weightedInput = neuron.input
        if (addNoise) {
            weightedInput += noiseGenerator.sampleDouble()
        }
        neuron.activation = type.valueOf(weightedInput, upperBound, lowerBound, slope)
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        val array = layer as NeuronArray
        val weightedInputs = array.inputs.clone()
        if (addNoise) {
            weightedInputs.addi(noiseGenerator.sampleDouble(array.size))
        }
        array.activations = type.valueOf(weightedInputs, lowerBound, upperBound, slope)
    }

    override fun copy(): SigmoidalRule {
        var sr = SigmoidalRule()
        sr = super.copy(sr) as SigmoidalRule
        return sr
    }

    override fun getDerivative(input: Double): Double {
        return type.derivVal(input, upperBound, lowerBound, upperBound - lowerBound)
    }

    override val name: String
        get() = "Sigmoidal (Discrete)"

}