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
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * For each source neuron, create a fixed number of connections to or from target neurons (fixed indegree vs. fixed
 * outdegree).
 *
 * @author Jeff Yoshimi
 */
class FixedDegree(

    /**
     * The number of connections allowed to or from each neuron.
     */
    @UserParameter(
        label = "Degree",
        description = "Maximum degree of connections per neuron",
        minimumValue = 0.0,
        order = 30
    )
    var degree: Int = 2,

    @UserParameter(
        label = "Indegree / Outdegree",
        description = "Make connections 'inward' (connections sent in to each neuron) or 'outward' (connections " +
                "radiating out from each neuron).",
        order = 10
    )
    var direction: Direction = Direction.IN,

    @UserParameter(
        label = "Use radius",
        description = "If true, only connect within a radius",
        order = 20
    )
    var useRadius: Boolean = false,

    radius: Double = 200.0,

    @UserParameter(
        label = "Allow self connections",
        description = "Allow synapses from neurons to themselves",
        order = 50
    )
    var allowSelfConnections: Boolean = false

    ) : ConnectionStrategy(), EditableObject {

    var radius by GuiEditable(
        description = "Radius within which to make connections",
        initValue = radius,
        conditionallyEnabledBy = FixedDegree::useRadius,
        order = 30
    )

    override fun connectNeurons(
        network: Network,
        source: List<Neuron>,
        target: List<Neuron>,
        addToNetwork: Boolean
    ): List<Synapse> {
        val syns = if (useRadius) {
            createFixedDegreeInRadiusSynapses(source, target, degree, radius, direction, allowSelfConnections)
        } else {
            createFixedDegreeSynapses(source, target, degree, direction, allowSelfConnections)
        }
        polarizeSynapses(syns, percentExcitatory)
        if (addToNetwork) {
            network.addNetworkModelsAsync(syns)
        }
        return syns
    }

    override val name = "Fixed degree"

    override fun toString(): String {
        return name
    }

    override fun copy(): FixedDegree {
        return FixedDegree(degree, direction, useRadius, radius, allowSelfConnections).also {
            commonCopy(it)
        }

    }

}

/**
 * For each neuron in [src] connect to or from at most a fixed number [degree] of neurons in [tar].
 */
fun createFixedDegreeSynapses(
    src: List<Neuron>,
    tar: List<Neuron>,
    degree: Int,
    direction: Direction = Direction.IN,
    allowSelfConnection: Boolean = false
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    src.forEach { n -> syns.addAll(n.createToNSynapses(tar, degree, direction, allowSelfConnection)) }
    return syns
}

/**
 * Check in a radius and within it make fixed degree connections as using [Neuron.createToNSynapses]
 */
fun createFixedDegreeInRadiusSynapses(
    src: List<Neuron>,
    tar: List<Neuron>,
    degree: Int,
    radius: Double,
    direction: Direction = Direction.IN,
    allowSelfConnection: Boolean = false
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    src.forEach { n -> syns.addAll(n.createToNSynapses(n.getNeuronsInRadius(tar, radius),
        degree, direction,
        allowSelfConnection)) }
    return syns
}

/**
 * Connect a neuron to N other neurons, in a provided pool of neurons.
 */
fun Neuron.createToNSynapses(
    pool: List<Neuron>,
    N: Int,
    direction: Direction = Direction.IN,
    allowSelfConnection: Boolean = false,
    randomizer: ProbabilityDistribution = NormalDistribution(0.0, 1.0)
): List<Synapse> {
    return pool.shuffled()
        .filter { otherNeuron ->
            if (!allowSelfConnection) this != otherNeuron else true
        }
        .take(N)
        .map { otherNeuron ->
            if (direction == Direction.IN) {
                Synapse(otherNeuron, this, otherNeuron.polarity.value(randomizer.sampleDouble()))
            } else {
                Synapse(this, otherNeuron, this.polarity.value(randomizer.sampleDouble()))
            }
        }
}
