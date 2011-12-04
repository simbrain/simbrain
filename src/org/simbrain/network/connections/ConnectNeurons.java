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

import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Subclasses create connections (collections of synapses) between groups of
 * neurons.
 *
 * @author jyoshimi
 */
public abstract class ConnectNeurons {

    /** The network whose neurons are to be connected. */
    protected Network network;

    /**
     * The source group of neurons, generally from which connections will be
     * made.
     */
    protected List<? extends Neuron> sourceNeurons;

    /**
     * The target group of neurons, generally to which connections will be made.
     */
    protected List<? extends Neuron> targetNeurons;

    /**
     * Default constructor.
     *
     * @param network network to receive  connections
     * @param neurons source neurons
     * @param neurons2 target neurons
     */
    public ConnectNeurons(final Network network, final List<? extends Neuron> neurons, final List<? extends Neuron> neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
    }

    /**
     * This parameter-free constructor is used in the desktop.  User:
     *  - Picks a connection style in the GUI
     *  - Selects source and target neurons
     *  - Invokes connection.
     */
    public ConnectNeurons() {
    }

    /**
     * Apply connection using specified parameters.
     *
     * @param network reference to parent network
     * @param neurons source neurons
     * @param neurons2 target neurons
     */
    public void connectNeurons(final Network network,
            final List<Neuron> neurons, final List<Neuron> neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
        connectNeurons();
    }

    /**
     * Connect the source to the target neurons using some method.
     */
    public abstract void connectNeurons();

}
