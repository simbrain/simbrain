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
package org.simbrain.network

import org.simbrain.network.core.Network
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

/**
 * Network component.
 */
class NetworkComponent : WorkspaceComponent {
    /**
     * Reference to root network, the main model network.
     */
    var network: Network = Network()
        private set

    /**
     * Create a new network component.
     *
     * @param name name
     */
    constructor(name: String?) : super(name!!) {
        init()
    }

    /**
     * Create a new network component.
     *
     * @param name    name of network
     * @param network the network being created
     */
    constructor(name: String?, network: Network) : super(name!!) {
        this.network = network
        init()
    }

    /**
     * Initialize attribute types and listeners.
     */
    private fun init() {
        val event = network.events

        event.modelAdded.on { m ->
            setChangedSinceLastSave(true)
            if (m is AttributeContainer) {
                fireAttributeContainerAdded(m)
            }
            if (m is NeuronGroup) {
                m.neuronList.map { addedContainer ->
                    this.fireAttributeContainerAdded(
                        addedContainer
                    )
                }
            }
        }

        event.modelRemoved.on { m ->
            setChangedSinceLastSave(true)
            if (m is AttributeContainer) {
                fireAttributeContainerRemoved(m)
            }
            if (m is NeuronGroup) {
                m.neuronList.forEach { removedContainer ->
                    this.fireAttributeContainerRemoved(
                        removedContainer
                    )
                }
            }
        }


        //        event.onNeuronsUpdated(l -> setChangedSinceLastSave(true));
        //
        //        event.onTextAdded(t -> setChangedSinceLastSave(true));
        //
        //        event.onTextRemoved(t -> setChangedSinceLastSave(true));
    }

    override val attributeContainers: List<AttributeContainer>
        get() = network.allModels.filterIsInstance<AttributeContainer>()

    override fun save(output: OutputStream, format: String?) {
        getNetworkXStream().toXML(network, output)
    }

    /**
     * Returns a copy of this NetworkComponent.
     *
     * @return the new network component
     */
    fun copy(): NetworkComponent {
        val ret = NetworkComponent("Copy of $name", network.copy())
        return ret
    }

    override suspend fun update() {
        network.updateSuspend(name)
    }

    override val xml: String
        get() = getSimbrainXStream().toXML(network)

    companion object {
        @JvmStatic
        fun open(input: InputStream?, name: String?, format: String?): NetworkComponent {
            val newNetwork = getNetworkXStream().fromXML(input) as Network
            return NetworkComponent(name, newNetwork)
        }
    }
}
