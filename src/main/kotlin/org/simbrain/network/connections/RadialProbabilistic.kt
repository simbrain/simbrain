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
import org.simbrain.network.util.SimnetUtils
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * For each neuron, consider every neuron in a radius and make excitatory and inhibitory synapses with them according to
 * some probability.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class RadialProbabilistic(

    // TODO: Implement excitatory vs. inhib probs
    // TODO: Check polarity respected
    // TODO: Distinguish EE, EI, etc?

    /**
     * Probability of making connections to neighboring excitatory neurons. Also used for
     * neurons with no polarity.
     */
    @UserParameter(
        label = "Exc. Probability",
        description = "Probability connections will be made to neighbor excitatory (or non-polar) neurons ",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 5
    )
    var excitatoryProbability: Double = .8,

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    @UserParameter(
        label = "Inh. Probability",
        description = "Probability connections will be made to neighbor inhibitory neurons ",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 6
    )
    var inhibitoryProbability: Double = .8,

    /**
     * Radius within which to connect excitatory excNeurons.
     */
    @UserParameter(
        label = "Exc. Radius",
        description = "Distance to search for excitatory neurons to connect to",
        minimumValue = 0.0,
        order = 3
    )
    var excitatoryRadius: Double = 100.0,

    /**
     * Radius within which to connect inhibitory excNeurons.
     */
    @UserParameter(
        label = "Inh. Radius",
        description = "Distance to search for inhibitory neurons to connect to",
        minimumValue = 0.0,
        order = 4
    )
    var inhibitoryRadius: Double = 80.0,

    @UserParameter(
        label = "Allow self connections",
        description = "Allow synapses from neurons to themselves",
        order = 50
    )
    var allowSelfConnections: Boolean = false

    ) : ConnectionStrategy(), EditableObject {

    /**
     * Radial simple sets the polarity implicitly.
     */
    override val usesPolarity: Boolean
        get() = false

    /**
     * Connect neurons.
     */
    override fun connectNeurons(
        network: Network,
        source: List<Neuron>,
        target: List<Neuron>,
        addToNetwork: Boolean
    ): List<Synapse> {
        val exc = createProbabilisticallySynapses(source, target, excitatoryProbability,
            excitatoryRadius, allowSelfConnections, NormalDistribution(1.0,.1)
        )
        val inh = createProbabilisticallySynapses(source, target, excitatoryProbability,
            excitatoryRadius, allowSelfConnections, NormalDistribution(-1.0,0.1)
        )
        val syns = exc + inh
        if (addToNetwork) {
            network.addNetworkModelsAsync(syns)
        }
        return syns
    }


    override val name = "Radial (Probabilistic)"

    override fun toString(): String {
        return name
    }

    override fun copy(): RadialProbabilistic {
        return RadialProbabilistic(
            excitatoryProbability, inhibitoryProbability, excitatoryRadius, inhibitoryRadius, allowSelfConnections
        ).also {
            commonCopy(it)
        }
    }

}

fun createProbabilisticallySynapses(
    src: List<Neuron>,
    tar: List<Neuron>,
    prob: Double,
    radius: Double,
    allowSelfConnection: Boolean = false,
    randomizer: ProbabilityDistribution = NormalDistribution(0.0, 1.0)
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    src.forEach { n -> syns.addAll(n.createProbabilisticallySynapses(tar, prob, radius)) }
    return syns
}

/**
 * Connect a neuron to N other neurons, in a provided pool of neurons, using a provided probability
 */
fun Neuron.createProbabilisticallySynapses(
    pool: List<Neuron>,
    prob: Double,
    radius: Double,
    allowSelfConnection: Boolean = false,
    randomizer: ProbabilityDistribution = NormalDistribution(0.0, 1.0)
): List<Synapse> {
    return getNeuronsInRadius(pool, radius)
        .filter { otherNeuron ->
            if (!allowSelfConnection) this != otherNeuron else true
        }
        .filter { Math.random() < prob }
        .map { otherNeuron ->
            Synapse(this, otherNeuron, otherNeuron.polarity.value(randomizer.sampleDouble()))
        }
}


fun Neuron.getNeuronsInRadius(neighbors: List<Neuron>, radius: Double): List<Neuron> {
    val ret = ArrayList<Neuron>()
    for (neuron in neighbors) {
        if (SimnetUtils.getEuclideanDist(this, neuron) < radius) {
            ret.add(neuron)
        }
    }
    return ret
}

/**
 * Are neurons within a given radius being connected <emp>to</emp> the neuron in
 * question (IN) or are they being connected <emp>from</emp> the neuron in
 * question (OUT)?
 */
enum class Direction(private val description: String) {
    OUT("Outdegree"), IN("Indegree");

    override fun toString(): String {
        return description
    }
}