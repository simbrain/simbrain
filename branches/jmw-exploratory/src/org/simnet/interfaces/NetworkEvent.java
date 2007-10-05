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
package org.simnet.interfaces;

import java.util.EventObject;

/**
 * Network event.  Currently adding or deleting a neuron.
 */
public final class NetworkEvent
    extends EventObject {

    /** default serial id */
	private static final long serialVersionUID = 1L;

	/** Reference to neuron. */
    private Neuron neuron;

    /** Reference to neuron. */
    private Neuron oldNeuron;

    /** Reference to neuron. */
    private Synapse synapse;

    /** Reference to neuron. */
    private Synapse oldSynapse;

    /** Reference to subnetwork. */
    private Network subnet;

    /** Reference to group. */
    private Group group;

    /** Reference to group. */
    private Group oldGroup;

    /**
     * Create a new model event.
     *
     * @param net reference to network firing event
     * @param neuron reference to the neuron this event concerns
     */
    public NetworkEvent(final Network net, final Neuron neuron) {
        super(net);
        this.neuron = neuron;
    }

    /**
     * Create a new model event.
     *
     * @param net reference to network firing event
     * @param synapse reference to the synapse this event concerns
     */
    public NetworkEvent(final Network net, final Synapse synapse) {
        super(net);
        this.synapse = synapse;
    }

    /**
     * Create a new model event.
     *
     * @param net reference to network firing event
     * @param neuron refrence to to the neuron this event concerns
     * @param oldNeuron reference to the old neuron this event concerns
     */
    public NetworkEvent(final Network net, final Neuron oldNeuron,  final Neuron neuron) {
        super(net);
        this.neuron = neuron;
        this.oldNeuron = oldNeuron;
    }

    /**
     * Create a new model event.
     *
     * @param net reference to network firing event
     * @param synapse reference to the synapse this event concerns
     * @param oldSynapse reference to the old synapse this event concerns
     */
    public NetworkEvent(final Network net, final Synapse oldSynapse, final Synapse synapse) {
        super(net);
        this.synapse = synapse;
        this.oldSynapse = oldSynapse;
    }

   /**
    * Create a new model event.
    *
    * @param parentNet reference to network firing event
    * @param added reference to the new subnetwork being added
    */
    public NetworkEvent(final Network parentNet, final Network added) {
        super(parentNet);
        this.subnet = added;
    }

    /**
     * Group delted or added.
     * 
     * @param parentNet network firing the event
     * @param added group added or delete
     */
    public NetworkEvent(final Network parentNet, final Group added) {
        super(parentNet);
        this.group = added;
    }

    /**
     * Group event.
     *
     * @param parentNet network firing event.
     * @param oldGroup old group.
     * @param group new group.
     */
    public NetworkEvent(final Network parentNet, final Group oldGroup, final Group group) {
        super(parentNet);
        this.group = group;
        this.oldGroup = oldGroup;
    }

    /**
     * @return Returns the neuron.
     */
    public Neuron getNeuron() {
        return neuron;
    }

    /**
     * @return Returns the synapse.
     */
    public Synapse getSynapse() {
        return synapse;
    }

    /**
     * @return Returns the oldNeuron.
     */
    public Neuron getOldNeuron() {
        return oldNeuron;
    }

    /**
     * @return Returns the oldSynapse.
     */
    public Synapse getOldSynapse() {
        return  oldSynapse;
    }

    /**
     * @return Returns the subnet.
     */
    public Network getSubnet() {
        return subnet;
    }

    /**
     * @param subnet The subnet to set.
     */
    public void setSubnet(final Network subnet) {
        this.subnet = subnet;
    }

    /**
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * @return the oldGroup
     */
    public Group getOldGroup() {
        return oldGroup;
    }


}