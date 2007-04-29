/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.simbrain.workspace.Workspace;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
import org.simbrain.world.WorldListener;
import org.simnet.NetworkThread;
import org.simnet.coupling.Coupling;
import org.simnet.coupling.InteractionMode;


/**
 * <b>Group</b> A meaningful group of neurons, synapses, and later, subnetworks.  These
 * elements may be part of other subnetworks.  Extending this class and overriding the update
 * function gives it functoinality.
 *
 * This extends Network not because these should behave like networks but mainly to take advantage of utility
 * methods associated with the Network class.
 */
public abstract class Group extends Network {

    /** Reference to root network. */
    private RootNetwork rootNetwork = null;

    /** Array list of neurons. */
    private ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

    /**
     * @return the neuronList
     */
    public ArrayList<Neuron> getNeuronList() {
        return neuronList;
    }

    /**
     * @return the rootNetwork
     */
    public RootNetwork getRootNetwork() {
        return rootNetwork;
    }

    /**
     * @param rootNetwork the rootNetwork to set
     */
    public void setRootNetwork(final RootNetwork rootNetwork) {
        this.rootNetwork = rootNetwork;
    }

}
