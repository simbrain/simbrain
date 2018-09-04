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

import java.util.List;

/**
 * Manage quick connection preferences, where a connection is applied using key
 * commands. Basically a repository of connection objects whose settings can be
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
     * The all to all connector.
     */
    private final AllToAll allToAll = new AllToAll();

    /**
     * The one to one connector.
     */
    private final OneToOne oneToOne = new OneToOne();

    /**
     * The Gaussian connector.
     */
    private final RadialGaussian radial = new RadialGaussian();

    /**
     * The Radial connector.
     */
    private final RadialSimple radialSimple = new RadialSimple();

    /**
     * The sparse connector.
     */
    private final Sparse sparse = new Sparse();

    /**
     * The current connection object.
     */
    private ConnectNeurons currentConnector;

    /**
     * Construct the quick connection manager.
     */
    public QuickConnectionManager() {
        currentConnector = allToAll;
    }

    /**
     * @return the connection objects
     */
    public ConnectNeurons[] getConnectors() {
        return new ConnectNeurons[]{allToAll, oneToOne, radial, radialSimple, sparse};
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

    public ConnectNeurons getCurrentConnector() {
        return currentConnector;
    }

    public void setCurrentConnector(ConnectNeurons currentConnector) {
        this.currentConnector = currentConnector;
    }

    public AllToAll getAllToAll() {
        return allToAll;
    }

    public OneToOne getOneToOne() {
        return oneToOne;
    }

    public Sparse getSparse() {
        return sparse;
    }

    public RadialGaussian getRadial() {
        return radial;
    }

    public RadialSimple getRadialSimple() {
        return radialSimple;
    }
}
