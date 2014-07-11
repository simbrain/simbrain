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

import org.simbrain.network.groups.SynapseGroup;

/**
 * Subclasses create connections (collections of synapses) between groups of
 * neurons.
 *
 * TODO: Keep? If not, demote to interface?
 *
 * @author Zach Tosi
 */
public abstract class ConnectNeurons {

    /**
     * Default constructor.
     */
    public ConnectNeurons() {

    }

    /**
     * Apply connection using specified parameters.
     *
     * @param network
     *            reference to parent network
     * @param neurons
     *            source neurons
     * @param neurons2
     *            target neurons
     * @param looseSynapses
     *            true if the connections that are being created are "loose";
     *            false if the connections will be placed in a synapse group
     * @return the synapses that were created
     */
    public abstract void connectNeurons(final SynapseGroup synGroup);

}
