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
 * Does not use excitatory ratio
 *
 * TODO: Add full repetoire of probabilities EE,EI,IE,II...
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class RadialSimple() : ConnectionStrategy(), EditableObject {

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
     * Are neurons within a given radius being connected <emp>to</emp> the neuron in
     * question ([.IN] or are they being connected <emp>from</emp> the neuron in
     * question ([.OUT])? Equivalently is the neuron for which are checking for
     * other neurons within a given distance the target of the connections that will
     * made or the source?
     */
    enum class SelectionStyle(private val description: String) {
        OUT("Outward"), IN("Inward");
        override fun toString(): String {
            return description
        }
    }

    /**
     * Should connections be selected randomly from a given neighborhood or in a prescribed way
     * based on the in-degree?
     */
    @UserParameter(
        label = "Connection Method",
        description = "Make local connections based on a specified in-degree (determnistic) or randomly (probabilistic)",
        order = 1
    )
    var conMethod = ConnectStyle.PROBABILISTIC

    @UserParameter(
        label = "Inward / Outward",
        description = "Are the connections to be made 'inward' (connections sent in to each neuron) or 'outward' (connections radiating out from each neuron).",
        order = 2
    )
    var selectMethod = SelectionStyle.IN

    /**
     * Whether to allow self-connections.
     */
    @UserParameter(
        label = "Self-Connections Allowed ",
        description = "Can there exist synapses whose source and target are the same?",
        order = 9
    )
    var isAllowSelfConnections = false

    /**
     * Template synapse for excitatory synapses.
     */
    var baseExcitatorySynapse = Synapse.getTemplateSynapse()

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
    var excitatoryProbability = .8

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
    var inhibitoryProbability = .8

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
    var excCons = 5

    /**
     * Radius within which to connect excitatory excNeurons.
     */
    @UserParameter(
        label = "Exc. Radius",
        description = "Distance to search for excitatory neurons to connect to",
        minimumValue = 0.0,
        order = 3
    )
    var excitatoryRadius = 100.0

    /**
     * Template synapse for inhibitory synapses.
     */
    var baseInhibitorySynapse = Synapse.getTemplateSynapse()

    /**
     * Radius within which to connect inhibitory excNeurons.
     */
    @UserParameter(
        label = "Inh. Radius",
        description = "Distance to search for inhibitory neurons to connect to",
        minimumValue = 0.0,
        order = 4
    )
    var inhibitoryRadius = 80.0

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
    var inhCons = 5

    /**
     * Radial simple sets the polarity implicitly.
     */
    override val overridesPolarity: Boolean
        get() = true

    // TODO: Keep?
    /**
     * Reference to network in which radial connections will be made on loose
     * synapses.
     */
    private var network: Network? = null

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
     * Connect neurons. TODO: source / target distinction not needed.
     */
    override fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse> {
        this.network = network
        val neurons = HashSet(source + target)

        val grouped = neurons.groupBy { it.polarity }
        excNeurons = grouped[Polarity.EXCITATORY]
        inhNeurons = grouped[Polarity.INHIBITORY]
        nonPolarNeurons = grouped[Polarity.BOTH]

        val syns = ArrayList<Synapse>()
        neurons.forEach { n ->
            makeExcitatory(n, syns, true)
            makeInhibitory(n, syns, true)
        }
        network.addNetworkModels(syns)
        return syns

    }

    override fun connectNeurons(synGroup: SynapseGroup) {

        // No implementation yet.
        val target = synGroup!!.targetNeurons
        val source = synGroup!!.sourceNeurons
        val syns = ArrayList<Synapse>()

        if (selectMethod == SelectionStyle.IN) {
            for (tar in target) {
                makeConnects(tar, source, syns)
            }
        } else {
            for (src in source) {
                makeConnects(src, target, syns)
            }
        }
        for (s in syns) {
            synGroup!!.addNewSynapse(s)
        }
    }

    /**
     * Make an inhibitory neuron, in the sense of connecting this neuron with
     * surrounding excNeurons via excitatory connections.
     *
     * @param neuron
     */
    private fun makeInhibitory(neuron: Neuron, syns: MutableList<Synapse>?, looseSynapses: Boolean) {

        // TODO: abstract what is common to make excitatory?
        var degreeCounter = 0
        val neusInRadius = SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, inhibitoryRadius)
        neusInRadius.addAll(SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, inhibitoryRadius))
        if (conMethod == ConnectStyle.DETERMINISTIC) {
            Collections.shuffle(neusInRadius)
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
                        synapse.strength = Math.random()
                    } else {
                        synapse = Synapse(neuron, otherNeu)
                    }
                    syns?.add(synapse)
                    // if (looseSynapses) {
                    //     network!!.addNetworkModel(synapse)
                    // } else {
                    //     syns?.add(synapse)
                    // }
                }
            } else {
                var synapse: Synapse = if (selectMethod == SelectionStyle.IN) 
                    Synapse(otherNeu, neuron) else {
                    Synapse(neuron, otherNeu)
                }
                synapse.strength = -Math.random()
                // if (looseSynapses) {
                //     network!!.addNetworkModel(synapse)
                // } else {
                //     syns?.add(synapse)
                // }
                degreeCounter++
                if (degreeCounter >= inhCons) {
//                    network.fireSynapsesUpdated(); // TODO: [event]
                    break
                }
            }
            if (network != null) {
//                network.fireSynapsesUpdated(); // TODO: [event]
            }
        }
    }

    /**
     *  Connect this neuron with surrounding neurons via excitatory connections.
     *
     * @param neuron neuron neuron
     */
    private fun makeExcitatory(neuron: Neuron, syns: MutableList<Synapse>?, looseSynapses: Boolean) {
        val degreeCounter = 0
        // TODO: Currently broken since it only focuses on non-polar neurons
        val neusInRadius = SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, excitatoryRadius)
        // neusInRadius.addAll(SimnetUtils.getNeuronsInRadius(neuron, nonPolarNeurons, excitatoryRadius))
        if (conMethod == ConnectStyle.DETERMINISTIC) {
            Collections.shuffle(neusInRadius)
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
                    var synapse: Synapse
                    synapse = if (selectMethod == SelectionStyle.IN) Synapse(otherNeu, neuron) else {
                        Synapse(neuron, otherNeu)
                    }
                    synapse.strength = Math.random()
                    syns?.add(synapse)
                    // if (looseSynapses) {
                    //     // TODO: Adding a model directly rather than
                    //     network.addNetworkModel(synapse);
                    // } else {
                    //     if (syns != null)
                    //         syns.add(synapse);
                    // }
                }
            } else {
                var synapse: Synapse
                synapse = if (selectMethod == SelectionStyle.IN) Synapse(otherNeu, neuron) else {
                    Synapse(neuron, otherNeu)
                }
                synapse.strength = Math.random()
                syns?.add(synapse)

                // if (looseSynapses) {
                //     network.addNetworkModel(synapse);
                // } else {
                //     if (syns != null)
                //         syns.add(synapse);
                // }
                // degreeCounter++;
                // if(degreeCounter >= excCons) {
                //     network.fireSynapsesUpdated();
                //     break;
                // }
            }
            // if (network != null) {
            //     network.fireSynapsesUpdated();
            // }
        }
    }


    /**
     * Makes connections between a neuron and some other neurons and returns a synapse list.
     * Accounts for connection and selection styles of various kinds and polarity.
     * @param neu
     * @param others
     * @param retList
     * @return
     */
    fun makeConnects(neu: Neuron?, others: List<Neuron?>, retList: MutableList<Synapse>): List<Synapse> {
        var others = others
        others = SimnetUtils.getNeuronsInRadius(neu, others, excitatoryRadius)
        if (others.isEmpty()) {
            return retList
        }
        return if (conMethod == ConnectStyle.PROBABILISTIC) {
            val p = if (neu!!.polarity !== Polarity.INHIBITORY) excitatoryProbability else inhibitoryProbability
            connectProb(neu, others, retList, selectMethod, p)
        } else {
            val noCons = if (neu!!.polarity !== Polarity.INHIBITORY) excCons else inhCons
            connectDet(neu, others, retList, selectMethod, noCons)
        }
    }


    override fun getName(): String {
        return "Radial (Simple)"
    }

    override fun toString(): String {
        return name
    }

    companion object {
        /**
         * Connects a neuron to a list of possible neurons to connect to probabilistically. Synapses
         * are assigned weight values based on source polarity.
         * @param n The neuron of interest. If selection style is IN, it is the neuron the others send
         * connections to. If selection style is OUT, it is the neuron sending connections to the others.
         * @param others
         * @param retList
         * @param selectionStyle
         * @param p
         * @return
         */
        private fun connectProb(
            n: Neuron?, others: List<Neuron?>,
            retList: MutableList<Synapse>,
            selectionStyle: SelectionStyle, p: Double
        ): List<Synapse> {
            for (o in others) {
                if (Math.random() < p) {
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
         * @param others
         * @param retList
         * @param selectionStyle
         * @param N
         * @return
         */
        private fun connectDet(
            n: Neuron?, others: List<Neuron?>,
            retList: MutableList<Synapse>,
            selectionStyle: SelectionStyle, N: Int
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
}