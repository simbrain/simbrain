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
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.EditableObject

/**
 * Connect every source neuron to every target neuron.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class AllToAll(

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    @UserParameter(
        label = "Self-Connections Allowed ",
        description = "Can there exist synapses whose source and target are the same?",
        order = 1
    )
    var isSelfConnectionAllowed: Boolean = false

) : ConnectionStrategy(), EditableObject {

    override fun getName(): String {
        return "All to All"
    }

    override fun toString(): String {
        return name
    }

    /**
     * Connects neurons such that every source neuron is connected to every
     * target neuron. The only exception to this case is if the source neuron
     * group is the target neuron group and self-connections are not allowed.
     *
     * @param synGroup the synapse group to which the synapses created by this
     * connection class will be added.
     */
    override fun connectNeurons(synGroup: SynapseGroup) {
        val syns = connectAllToAll(
            synGroup.sourceNeurons,
            synGroup.targetNeurons,
            synGroup.isRecurrent,
            isSelfConnectionAllowed
        )
        // Set the capacity of the synapse group's list to accommodate the
        // synapses this group will add.
        synGroup.preAllocateSynapses(synGroup.sourceNeuronGroup.size() * synGroup.targetNeuronGroup.size())
        syns.forEach{s -> synGroup.addNewSynapse(s)}
    }

    override fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse> {
        val syns = connectAllToAll(source, target)
        network.addNetworkModels(syns)
        return syns
    }
}

/**
 * Connects every source neuron to every target neuron. The only exception
 * being that if the source and target neuron lists are the same, then no
 * connection will be made between a neuron and itself if self connections
 * aren't allowed. Will produce n^2 synapses if self connections are allowed
 * and n(n-1) if they are not.
 *
 * @param sourceNeurons       the source neurons
 * @param targetNeurons       the target neurons
 * @param recurrent           whether the source and target neurons overlap. Some
 * classes know ahead of time if the connection will be recurrent
 * and knowing this allows a slight performance improvement.
 * @param allowSelfConnection whether to allow self-connections
 * @param looseSynapses       whether the synapses being connected are loose or in
 * a synapse group
 * @return the synapses created.
 */
fun connectAllToAll(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    recurrent: Boolean = Utils.intersects(sourceNeurons, targetNeurons),
    allowSelfConnection: Boolean = false
): List<Synapse> {
    val syns = ArrayList<Synapse>((targetNeurons.size * sourceNeurons.size))
    // Optimization: separately handle case where we have to worry about
    // avoiding self-connections, so an equals check is required.
    if (recurrent && !allowSelfConnection) {
        for (source in sourceNeurons) {
            for (target in targetNeurons) {
                if (source != target) {
                    val s = Synapse(source, target)
                    s.strength = 1.0
                    syns.add(s)
                }
            }
        }
    } else {
        // The case where we don't need to worry about self-connections
        for (source in sourceNeurons) {
            for (target in targetNeurons) {
                val s = Synapse(source, target)
                s.strength = 1.0
                syns.add(s)
            }
        }
    }
    return syns
}
