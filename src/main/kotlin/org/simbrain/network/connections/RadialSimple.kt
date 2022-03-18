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
import org.simbrain.network.core.getLooseSynapse
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.util.SimnetUtils
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import java.util.*

/**
 * For each neuron, consider every neuron in an excitatory and inhibitory radius
 * from it, and make excitatory and inhibitory synapses with them according to
 * some probability. Inhibitory and excitatory synapses are created separately
 * and use separate parameters. Therefore the total number of connections that will
 * be made depends upon both sets of parameters.
 *
 * TODO: Add full repetoire of probabilities EE,EI,IE,II...
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class RadialSimple(

    /**
     * Should connections be selected randomly from a given neighborhood or in a prescribed way
     * based on the in-degree?
     */
    @UserParameter(
        label = "Connection Method",
        description = "Make local connections based on a specified in-degree (determnistic) or randomly (probabilistic)",
        order = 1
    )
    var conMethod: ConnectStyle = ConnectStyle.PROBABILISTIC,

    @UserParameter(
        label = "Inward / Outward",
        description = "Are the connections to be made 'inward' (connections sent in to each neuron) or 'outward' (connections radiating out from each neuron).",
        order = 2
    )
    var selectMethod: SelectionStyle = SelectionStyle.IN,

    /**
     * Whether to allow self-connections.
     */
    @UserParameter(
        label = "Self-Connections Allowed ",
        description = "Can there exist synapses whose source and target are the same?",
        order = 9
    )
    var isAllowSelfConnections: Boolean = false,

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
     * The number of connections allowed with excitatory (or non-polar) neurons. If there
     * are sufficient excitatory (or non-polar) neurons in a given neuron's neighborhood this
     * will be how many connections are made.
     */
    @UserParameter(
        label = "Num. Exc. Connections",
        description = "Maximum # of connections with exc. neurons",
        minimumValue = 0.0,
        order = 7
    )
    var excCons: Double = 5.0,

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

    /**
     * The number of connections allowed with inhibitory neurons. If there
     * are sufficient inhibitory neurons in a given neuron's neighborhood this
     * will be how many connections are made.
     */
    @UserParameter(
        label = "Num. Inh. Connections",
        description = "Maximum # of connections with inh. neurons",
        minimumValue = 0.0,
        order = 8
    )
    var inhCons: Double = 5.0


) : ConnectionStrategy(), EditableObject {

    /**
     * When connecting neurons within a radius of a given neuron they can be chosen
     * stochastically, based on some probability parameter ([.PROBABILISTIC]) or
     * deterministically based upon a predefined number of requested connections
     * ([.DETERMINISTIC]).
     */
    enum class ConnectStyle(private val description: String) {
        PROBABILISTIC("Probabilistic"), DETERMINISTIC("Deterministic");

        override fun toString(): String {
            return description
        }
    }


    /**
     * Radial simple sets the polarity implicitly.
     */
    override val overridesPolarity: Boolean
        get() = true

    /**
     * List containing neurons with excitatory polarity among the neurons selected
     * to form connections between
     */
    private var excNeurons: List<Neuron>? = null

    /**
     * List containing neurons with inhibitory polarity among the neurons selected
     * to form connections between
     */
    private var inhNeurons: List<Neuron>? = null

    /**
     * A list containing all non polar neurons among the neurons selected to form connections
     * between
     */
    private var nonPolarNeurons: List<Neuron>? = null

    /**
     * Connect neurons.
     */
    override fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse> {

        val neurons = HashSet(source + target)

        val grouped = neurons.groupBy { it.polarity }
        excNeurons = grouped[Polarity.EXCITATORY]
        inhNeurons = grouped[Polarity.INHIBITORY]
        nonPolarNeurons = grouped[Polarity.BOTH]

        val syns = ArrayList<Synapse>()
        neurons.forEach { n ->
            makeExcitatory(n, syns)
            makeInhibitory(n, syns)
        }
        network.addNetworkModels(syns)
        return syns

    }

    override fun connectNeurons(synGroup: SynapseGroup) {

        // No implementation yet.
        val target = synGroup.targetNeurons
        val source = synGroup.sourceNeurons
        val syns = ArrayList<Synapse>()

        // TODO: Why selection style here when used in make connects?
        if (selectMethod == SelectionStyle.IN) {
            target.forEach{tar ->  makeConnects(tar, source, syns)}
        } else {
            source.forEach{src ->  makeConnects(src, target, syns)}
        }
        syns.forEach{s -> synGroup.addNewSynapse(s)}

    }

    // TODO: Just remove the next two functions?

    /**
     * Make an inhibitory neuron, in the sense of connecting this neuron with
     * surrounding excNeurons via excitatory connections.
     *
     * @param neuron
     */
    private fun makeInhibitory(neuron: Neuron, syns: MutableList<Synapse>) {

        // TODO: abstract what is common to make excitatory?

        val neusInRadius = SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, inhibitoryRadius)
        neusInRadius.addAll(SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, inhibitoryRadius))
        if (conMethod == ConnectStyle.DETERMINISTIC) {
            neusInRadius.shuffle()
        }
        for (otherNeu in neusInRadius) {
            // Don't add a connection if there is already one present
            if (getLooseSynapse(neuron, otherNeu!!) != null) {
                continue
            }
            if (!isAllowSelfConnections) {
                if (neuron === otherNeu) {
                    continue
                }
            }
            if (conMethod == ConnectStyle.PROBABILISTIC) {
                if (Math.random() < inhibitoryProbability) {
                    var synapse: Synapse
                    if (selectMethod == SelectionStyle.IN) {
                        synapse = Synapse(otherNeu, neuron)
                        synapse.strength = -Math.random()
                    } else {
                        synapse = Synapse(neuron, otherNeu)
                    }
                    syns.add(synapse)
                }
            } else {
                val synapse: Synapse = if (selectMethod == SelectionStyle.IN)
                    Synapse(otherNeu, neuron) else {
                    Synapse(neuron, otherNeu)
                }
                synapse.strength = -Math.random()
                syns.add(synapse)
            }
        }
    }

    /**
     *  Connect this neuron with surrounding neurons via excitatory connections.
     *
     * @param neuron neuron neuron
     */
    private fun makeExcitatory(neuron: Neuron, syns: MutableList<Synapse>) {
        // TODO: Currently broken since it only focuses on non-polar neurons
        val neusInRadius = SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, excitatoryRadius)
        // neusInRadius.addAll(SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, excitatoryRadius))
        if (conMethod == ConnectStyle.DETERMINISTIC) {
            neusInRadius.shuffle()
        }
        for (otherNeu in neusInRadius) {
            // Don't add a connection if there is already one present
            if (getLooseSynapse(neuron, otherNeu!!) != null) {
                continue
            }
            if (!isAllowSelfConnections) {
                if (neuron === otherNeu) {
                    continue
                }
            }
            if (conMethod == ConnectStyle.PROBABILISTIC) {
                if (Math.random() < excitatoryProbability) {
                    val synapse: Synapse = if (selectMethod == SelectionStyle.IN) Synapse(otherNeu, neuron) else {
                        Synapse(neuron, otherNeu)
                    }
                    synapse.strength = Math.random()
                    syns.add(synapse)
                }
            } else {
                val synapse: Synapse = if (selectMethod == SelectionStyle.IN) Synapse(otherNeu, neuron) else {
                    Synapse(neuron, otherNeu)
                }
                synapse.strength = Math.random()
                syns.add(synapse)
            }
        }
    }

    /**
     * Makes connections between a neuron and some other neurons and returns a synapse list.
     * Accounts for connection and selection styles of various kinds and polarity.
     */
    fun makeConnects(neu: Neuron, others: List<Neuron>, retList: MutableList<Synapse>): List<Synapse> {
        var others = others
        others = SimnetUtils.getNeuronsInRadius(neu, others, excitatoryRadius)
        if (others.isEmpty()) {
            return retList
        }
        // TODO: What about non-polar?
        return if (conMethod == ConnectStyle.PROBABILISTIC) {
            val p = if (neu.polarity !== Polarity.INHIBITORY) excitatoryProbability else inhibitoryProbability
            connectProb(neu, others, retList, selectMethod, p)
        } else {
            val noCons = if (neu.polarity !== Polarity.INHIBITORY) excCons else inhCons
            connectDet(neu, others, retList, selectMethod, noCons.toInt())
        }
    }

    override fun getName(): String {
        return "Radial (Simple)"
    }

    override fun toString(): String {
        return name
    }

    /**
     * Connects a neuron to a list of possible neurons to connect to probabilistically. Synapses
     * are assigned weight values based on source polarity.
     * @param n The neuron of interest. If selection style is IN, it is the neuron the others send
     * connections to. If selection style is OUT, it is the neuron sending connections to the others.
     */
    private fun connectProb(
        n: Neuron?, others: List<Neuron?>,
        retList: MutableList<Synapse>,
        selectionStyle: SelectionStyle,
        prob: Double
    ): List<Synapse> {
        for (o in others) {
            if (Math.random() < prob) {
                if (selectionStyle == SelectionStyle.IN) {
                    retList.add(Synapse(o, n, o!!.polarity.value(Math.random())))
                } else {
                    retList.add(Synapse(n, o, n!!.polarity.value(Math.random())))
                }
            }
        }
        return retList
    }

    /**
     * Connects a neuron to a list of possible neurons to connect to "deterministically".
     * An exact number of neurons to connect with are chosen randomly, but the number itself
     * is guaranteed. Synapses are assigned weight values based on source polarity.
     * @param n The neuron of interest. If selection style is IN, it is the neuron the others send
     * connections to. If selection style is OUT, it is the neuron sending connections to the others.
     */
    private fun connectDet(
        n: Neuron,
        others: List<Neuron>,
        retList: MutableList<Synapse>,
        selectionStyle: SelectionStyle,
        N: Int
    ): List<Synapse> {
        var N = N
        if (N > others.size) {
            N = others.size
        } else {
            Collections.shuffle(others)
        }
        for (ii in 0 until N) {
            val o = others[ii]
            if (selectionStyle == SelectionStyle.IN) {
                retList.add(Synapse(o, n, o!!.polarity.value(Math.random())))
            } else {
                retList.add(Synapse(n, o, n!!.polarity.value(Math.random())))
            }
        }
        return retList
    }

}

/**
 * Are neurons within a given radius being connected <emp>to</emp> the neuron in
 * question (IN) or are they being connected <emp>from</emp> the neuron in
 * question (OUT)? Equivalently is the neuron for which are checking for
 * other neurons within a given distance the target of the connections that will
 * be made or the source?
 */
enum class SelectionStyle(private val description: String) {
    OUT("Outward"), IN("Inward");

    override fun toString(): String {
        return description
    }
}