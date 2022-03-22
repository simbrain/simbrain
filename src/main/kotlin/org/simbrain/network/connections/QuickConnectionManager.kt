/*
 * Part of Simbrain--a java-based neural network kit
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
import org.simbrain.util.SimbrainPreferences
import org.simbrain.util.Utils

/**
 * Manage quick connection preferences, where a connection is applied using key
 * commands. A repository of connection objects whose settings can be
 * changed.
 *
 * Not thread-safe. This class is used by gui methods, and is not intended to be
 * used in settings where concurrent threads are simultaneously building network
 * objects.
 *
 * @author Jeff Yoshimi
 */
class QuickConnectionManager {

    /**
     * The current connection object.
     */
    private var currentConnector: ConnectionStrategy? = null

    /**
     * Construct the quick connection manager.
     */
    init {

        // Set the current connection strategy based on user preferences
        val xml: String? = SimbrainPreferences.getString("quickConnector")
        if ((xml == null) || xml.isEmpty() || !xml.startsWith("<org")) {
            // If no viable preferences found, default to All to All
            currentConnector = AllToAll()
        } else {
            currentConnector = Utils.getSimbrainXStream().fromXML(xml) as ConnectionStrategy?
        }
    }

    /**
     * Apply the current connection object to indicated source and target
     * neurons.
     *
     * @param net network containing neurons to connect
     * @param source source neurons
     * @param target target neurons
     */
    fun applyCurrentConnection(net: Network, source: List<Neuron>, target: List<Neuron>) {
        val retList: List<Synapse> = currentConnector!!.connectNeurons(net, source, target)
        polarizeSynapses(retList, currentConnector!!.excitatoryRatio)
        if (currentConnector!!.isUseExcitatoryRandomization) {
            randomizeExcitatorySynapses(retList, currentConnector!!.exRandomizer)
        }
        if (currentConnector!!.isUseInhibitoryRandomization) {
            randomizeInhibitorySynapses(retList, currentConnector!!.inRandomizer)
        }
    }

    fun getCurrentConnector(): ConnectionStrategy? {
        return currentConnector
    }

    fun setCurrentConnector(currentConnector: ConnectionStrategy?) {
        // Store the preferences using xml
        SimbrainPreferences.putString("quickConnector", Utils.getSimbrainXStream().toXML(currentConnector))
        this.currentConnector = currentConnector
    }
}