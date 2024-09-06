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

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.bound
import org.simbrain.network.util.OrientationComparator
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.random.Random

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
    var connectOrientation: OrientationComparator = OrientationComparator.X_ORDER,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed), EditableObject {

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = createOneToOneSynapses(source, target, useBidirectionalConnections)
        polarizeSynapses(syns, percentExcitatory, random)
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
 * Connect neurons 1-1
 */
fun createOneToOneSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    useBidirectionalConnections: Boolean = false
): List<Synapse> {
    val sourceBounds = sourceNeurons.bound
    val targetBounds = targetNeurons.bound
    val sourceCenter = sourceBounds.center
    val targetCenter = targetBounds.center
    val (_, _, sw, sh) = sourceBounds
    val (_, _, tw, th) = targetBounds

    val isSourceVertical = sw < sh
    val isTargetVertical = tw < th

    val isReversedTarget = isSourceVertical != isTargetVertical &&
            (targetCenter - sourceCenter).let { (x, y) -> (x > 0 && y > 0) || (x < 0 && y < 0) }

    return sourceNeurons.sortedBy { if (isSourceVertical) it.y else it.x }
        .zip(targetNeurons.sortedBy { if (isTargetVertical) it.y else it.x }.let { if (isReversedTarget) it.reversed() else it })
        .flatMap { (source, target) ->
            buildList {
                add(Synapse(source, target))
                if (useBidirectionalConnections) {
                    add(Synapse(target, source))
                }
            }
        }

}