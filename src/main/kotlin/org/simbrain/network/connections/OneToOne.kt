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
class OneToOne(

    /**
     * If true, synapses are added in both directions.
     */
    @UserParameter(label = "Bi-directional", order = 2)
    var useBidirectionalConnections: Boolean = false,

    /**
     * Orientation of how to connect neurons.
     */
    @UserParameter(label = "Orientation", order = 1)
    var connectOrientation: OrientationComparator = OrientationComparator.X_ORDER

) : ConnectionStrategy(), EditableObject {

    override fun connectNeurons(
        network: Network,
        source: List<Neuron>,
        target: List<Neuron>,
        addToNetwork: Boolean
    ): List<Synapse> {
        val syns = createOneToOneSynapses(source, target, useBidirectionalConnections)
        polarizeSynapses(syns, percentExcitatory)
        if (addToNetwork) {
            network.addNetworkModelsAsync(syns)
        }
        return syns
    }

    override val name = "One to one"

    override fun toString(): String {
        return name
    }

    override fun copy(): OneToOne {
        return OneToOne(useBidirectionalConnections, connectOrientation).also {
            commonCopy(it)
        }
    }

}

/**
 * Returns a sorted list of neurons, given a comparator.
 */
private fun getSortedNeuronList(neuronList: List<Neuron>, comparator: OrientationComparator): List<Neuron> {
    val list = ArrayList<Neuron>()
    list.addAll(neuronList)
    Collections.sort(list, comparator)
    return list
}

/**
 * Connect neurons 1-1
 */
fun createOneToOneSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    useBidirectionalConnections: Boolean = false
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
            syns.add(synapse)
            // Allow neurons to be connected back to source.
            if (useBidirectionalConnections) {
                val espanys = Synapse(target, source)
                syns.add(espanys)
            }
        } else {
            break
        }
    }
    return syns
}