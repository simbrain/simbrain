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
package org.simbrain.network.connections;

import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author jyoshimi
 */
public class AllToAll extends ConnectNeurons {

    /**
     * "Template" synapse to be used when applying the connection.
     */
    private static Synapse baseSynapse = Synapse.getTemplateSynapse();

    /** Allows neurons to have a self connection. */
    private static boolean allowSelfConnection = true;

    /**
     * Construct all to all connection object.
     *
     * @param network parent network
     * @param neurons base neurons
     * @param neurons2 target neurons
     */
    public AllToAll(final Network network,
            final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public AllToAll() {
    }

    @Override
    public String toString() {
        return "All to all";
    }


    /** {@inheritDoc} */
    public void connectNeurons() {
        for (Neuron source : sourceNeurons) {
            for (Neuron target : targetNeurons) {
                // Don't add a connection if there is already one present
                if (Network.getSynapse(source, target) != null) {
                    continue;
                }
                if (!allowSelfConnection) {
                    if (source != target) {
                        Synapse synapse = baseSynapse
                                .instantiateTemplateSynapse(source, target, network);
                        network.addSynapse(synapse);
                    }
                } else {
                    Synapse synapse = baseSynapse.instantiateTemplateSynapse(
                            source, target, network);
                    network.addSynapse(synapse);
                }
            }
        }
    }

    /**
     * @return the baseSynapse
     */
    public static Synapse getBaseSynapse() {
        return baseSynapse;
    }

    /**
     * @param baseSynapse the baseSynapse to set
     */
    public static void setBaseSynapse(final Synapse theSynapse) {
        baseSynapse = theSynapse;
    }

    /**
     * @return the allowSelfConnection
     */
    public static boolean isAllowSelfConnection() {
        return allowSelfConnection;
    }

    /**
     * @param allowSelfConnection the allowSelfConnection to set
     */
    public static void setAllowSelfConnection(boolean allowSelfConnection) {
        AllToAll.allowSelfConnection = allowSelfConnection;
    }
}
