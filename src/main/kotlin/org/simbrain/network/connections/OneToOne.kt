/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.connections

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.util.OrientationComparator
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import java.util.*

/**
 * Connect each source neuron to a single target.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
class OneToOne : ConnectionStrategy(), EditableObject {
    /**
     * If true, synapses are added in both directions.
     */
    @UserParameter(label = "Bi-directional", order = 2)
    private val useBidirectionalConnections = false

    /**
     * Orientation of how to connect neurons.
     */
    @UserParameter(label = "Orientation", order = 1)
    private val connectOrientation = DEFAULT_ORIENTATION

    /**
     * Source and target neuron groups must have the same number of neurons.
     * A synapse is created such that every source neuron is connected to
     * exactly one target neuron (and vice versa if connections are
     * bidirectional).
     */
    override fun connectNeurons(synGroup: SynapseGroup) {
        val syns = connectOneToOne(synGroup.sourceNeurons, synGroup.targetNeurons, useBidirectionalConnections, false)
        for (s in syns) {
            synGroup.addNewSynapse(s)
        }
    }

    override fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse> {
        return connectOneToOne(source, target, useBidirectionalConnections, true)
    }

    override fun getName(): String {
        return "One to one"
    }

    override fun toString(): String {
        return name
    }

    companion object {
        /**
         * Default orientation used to make the connections.
         */
        var DEFAULT_ORIENTATION = OrientationComparator.X_ORDER

        /**
         * Returns a sorted list of neurons, given a comparator.
         *
         * @param neuronList the base list of neurons.
         * @param comparator the comparator.
         * @return the sorted list.
         */
        private fun getSortedNeuronList(neuronList: List<Neuron>, comparator: OrientationComparator): List<Neuron> {
            val list = ArrayList<Neuron>()
            list.addAll(neuronList)
            Collections.sort(list, comparator)
            return list
        }
        /**
         * @param sourceNeurons               the starting neurons
         * @param targetNeurons               the targeted neurons
         * @param useBidirectionalConnections the useBidirectionalConnections to set
         * @param looseSynapses               whether loose synapses are being connected
         * @return array of synpases
         */
        /**
         * Use this connection object to make connections.
         *
         * @param sourceNeurons the starting neurons
         * @param targetNeurons the targeted neurons
         * @return the new synapses
         */
        @JvmOverloads
        fun connectOneToOne(
            sourceNeurons: List<Neuron>,
            targetNeurons: List<Neuron>,
            useBidirectionalConnections: Boolean = false,
            looseSynapses: Boolean = true
        ): List<Synapse> {
            val srcWidth = OrientationComparator.findMaxX(sourceNeurons) - OrientationComparator.findMinX(sourceNeurons)
            val srcHeight =
                OrientationComparator.findMaxY(sourceNeurons) - OrientationComparator.findMinY(sourceNeurons)
            val tarWidth = OrientationComparator.findMaxX(targetNeurons) - OrientationComparator.findMinX(targetNeurons)
            val tarHeight =
                OrientationComparator.findMaxY(targetNeurons) - OrientationComparator.findMinY(targetNeurons)

            // Sort by axis of maximal variance
            val srcSortX = srcWidth > srcHeight
            val tarSortX = tarWidth > tarHeight
            val srcComparator: OrientationComparator
            val tarComparator: OrientationComparator

            // srcSortX XOR tarSortX means that one should be sorted vertically
            // and the other horizonally.
            if (srcSortX != tarSortX) {
                val midpointXSrc = OrientationComparator.findMidpointX(sourceNeurons)
                val midpointXTar = OrientationComparator.findMidpointX(targetNeurons)
                val midpointYSrc = OrientationComparator.findMidpointY(sourceNeurons)
                val midpointYTar = OrientationComparator.findMidpointY(targetNeurons)
                if (srcSortX) { // source is horizontal
                    // Go over source in regular or reverse order based on the
                    // relative positions of the source and target midpoints.
                    srcComparator =
                        if (midpointXSrc > midpointXTar) OrientationComparator.X_REVERSE else OrientationComparator.X_ORDER
                    // Go over target in regular or reverse order based on the
                    // relative positions of the source and target midpoints.
                    tarComparator =
                        if (midpointYSrc > midpointYTar) OrientationComparator.Y_ORDER else OrientationComparator.Y_REVERSE
                } else { // source is vertical
                    srcComparator =
                        if (midpointYSrc > midpointYTar) OrientationComparator.Y_REVERSE else OrientationComparator.Y_ORDER
                    tarComparator =
                        if (midpointXSrc > midpointXTar) OrientationComparator.X_ORDER else OrientationComparator.X_REVERSE
                }
            } else {
                // Either we are sorting both vertically or both horizontally...
                srcComparator = if (srcSortX) OrientationComparator.X_ORDER else OrientationComparator.Y_ORDER
                tarComparator = if (tarSortX) OrientationComparator.X_ORDER else OrientationComparator.Y_ORDER
            }
            val syns = ArrayList<Synapse>()
            val targets = getSortedNeuronList(targetNeurons, tarComparator).iterator()
            val sources = getSortedNeuronList(sourceNeurons, srcComparator).iterator()
            while (sources.hasNext()) {
                val source = sources.next()
                if (targets.hasNext()) {
                    val target = targets.next()
                    val synapse = Synapse(source, target)
                    if (looseSynapses) {
                        source.network.addNetworkModel(synapse)
                    }
                    syns.add(synapse)
                    // Allow neurons to be connected back to source.
                    if (useBidirectionalConnections) {
                        val espanys = Synapse(target, source)
                        if (looseSynapses) {
                            source.network.addNetworkModel(espanys)
                        }
                        syns.add(espanys)
                    }
                } else {
                    break
                }
            }
            return syns
        }
    }
}