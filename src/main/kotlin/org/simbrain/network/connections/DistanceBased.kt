/*
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
package org.simbrain.network.connections

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.SimnetUtils.getEuclideanDist
import org.simbrain.util.UserParameter
import org.simbrain.util.cartesianProduct
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.ExponentialDecayFunction
import org.simbrain.util.propertyeditor.EditableObject

class DistanceBased (

    /**
     * Amount to decay connection probabilty as a function of pixel distance
     */
    @UserParameter(
        label = "Distance Function",
        description = "Decay function for connectionprobability",
        isObjectType = true,
        order = 1)
    var decayFunction: DecayFunction = ExponentialDecayFunction()

) : ConnectionStrategy(), EditableObject {

    override fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse> {
        val syns = connectRadial(source, target, decayFunction)
        network.addNetworkModels(syns)
        return syns
    }

    override fun toString(): String {
        return getName()
    }


    override fun getName(): String {
        return "Distance Based"
    }
}

fun connectRadial (
    source: List<Neuron>,
    target: List<Neuron>,
    decay: DecayFunction
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    (source cartesianProduct target).forEach{ (src, tar) ->
        if (src != tar) {
            val p = decay.getScalingFactor(getEuclideanDist(src, tar))
            if (Math.random() < p) {
                syns.add(Synapse(src, tar))
            }
        }
    }
    return syns
}
