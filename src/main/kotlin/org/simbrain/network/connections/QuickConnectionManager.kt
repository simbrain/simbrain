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
package org.simbrain.network.connections;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage quick connection preferences, where a connection is applied using key
 * commands. A repository of connection objects whose settings can be
 * changed.
 * <p>
 * Not thread-safe. This class is used by gui methods, and is not intended to be
 * used in settings where concurrent threads are simultaneously building network
 * objects.
 *
 * @author Jeff Yoshimi
 */
public class QuickConnectionManager {

    /**
     * The current connection object.
     */
    private ConnectionStrategy currentConnector;

    /**
     * Construct the quick connection manager.
     */
    public QuickConnectionManager() {

        // Set the current connection strategy based on user preferences
        String xml = SimbrainPreferences.getString("quickConnector");
        if (xml == null || xml.isEmpty() || !xml.startsWith("<org")) {
            // If no viable preferences found, default to All to All
            currentConnector = new AllToAll();
        } else {
            currentConnector = (ConnectionStrategy) Utils.getSimbrainXStream().fromXML(xml);
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
    public void applyCurrentConnection(Network net, List<Neuron> source, List<Neuron> target) {
        List<Synapse> retList = currentConnector.connectNeurons(net, source, target);
        if (retList != null) {
            ConnectionUtilities.polarizeSynapses(retList, currentConnector.getExcitatoryRatio());
            if (currentConnector.isUseExcitatoryRandomization()) {
                ConnectionUtilities.randomizeExcitatorySynapses(retList, currentConnector.getExRandomizer());
            }
            if (currentConnector.isUseInhibitoryRandomization()) {
                ConnectionUtilities.randomizeInhibitorySynapses(retList, currentConnector.getInRandomizer());
            }
        }
    }

    public ConnectionStrategy getCurrentConnector() {
        return currentConnector;
    }

    public void setCurrentConnector(ConnectionStrategy currentConnector) {
        // Store the preferences using xml
        SimbrainPreferences.putString("quickConnector", Utils.getSimbrainXStream().toXML(currentConnector));
        this.currentConnector = currentConnector;
    }
}
