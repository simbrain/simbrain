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
import java.util.Iterator;


/**
 * <b>ComplexNetwok</b> contains lists of sub-networks, e.g. backprop, where the subnetworks are "layers"
 */
public abstract class ComplexNetwork extends Network {
    protected ArrayList networkList = new ArrayList();

    public void init() {
        super.init();

        for (int i = 0; i < networkList.size(); i++) {
            ((Network) networkList.get(i)).init();
            ((Network) networkList.get(i)).setNetworkParent(this);
        }
    }

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        updateAllNetworks();
    }

    public void updateAllNetworks() {
        Iterator i = networkList.iterator();

        while (i.hasNext()) {
            ((Network) i.next()).update();
        }
    }

    public void addNetwork(Network n) {
        networkList.add(n);
        n.setNetworkParent(this);
    }

    public Network getNetwork(int i) {
        return (Network) networkList.get(i);
    }

    public void debug() {
        super.debug();

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            System.out.println("\n" + getIndents() + "Sub-network " + (i + 1) + " (" + net.getType() + ")");
            System.out.println(getIndents() + "--------------------------------");
            net.debug();
        }
    }

    /**
     * Delete network, and any of its ancestors which thereby become empty
     */
    public void deleteNetwork(Network toDelete) {
        networkList.remove(toDelete);

        //If this is the last network in a subnetwork, remove the subnetwork
        if (networkList.size() == 0) {
            ComplexNetwork parent = (ComplexNetwork) getNetworkParent();

            if (parent != null) {
                parent.deleteNetwork(this);
            }
        }
    }

    /**
     * Delete neuron, and any of its ancestors which thereby become empty
     */
    public void deleteNeuron(Neuron toDelete) {
        //If this is a top-level neuron use the regular delete; if it is a neuron in a sub-net, use its parent's delete
        if (this == toDelete.getParentNetwork()) {
            super.deleteNeuron(toDelete);
        } else {
            toDelete.getParentNetwork().deleteNeuron(toDelete);
        }

        //The subnetwork "parent" this neuron is part of is empty, so remove it from the grandparent network
        Network parent = toDelete.getParentNetwork();

        if (parent.getNeuronCount() == 0) {
            ComplexNetwork grand_parent = (ComplexNetwork) parent.getNetworkParent();

            if (grand_parent != null) {
                grand_parent.deleteNetwork(parent);
            }
        }
    }

    /**
     * Add an array of networks and set their parents to this
     *
     * @param neurons list of neurons to add
     */
    public void addNetworkList(ArrayList networks) {
        for (int i = 0; i < networks.size(); i++) {
            Network n = (Network) networks.get(i);
            n.setNetworkParent(this);
            networkList.add(n);
        }
    }

    /**
     * @return Returns the networkList.
     */
    public ArrayList getNetworkList() {
        return networkList;
    }

    /**
     * @param networkList The networkList to set.
     */
    public void setNetworkList(ArrayList networkList) {
        this.networkList = networkList;
    }

    /**
     * Create "flat" list of neurons, which includes the top-level neurons plus all subnet neurons
     *
     * @return the flat llist
     */
    public ArrayList getFlatNeuronList() {
        ArrayList ret = new ArrayList();
        ret.addAll(neuronList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList toAdd;

            if (net instanceof ComplexNetwork) {
                toAdd = (ArrayList) ((ComplexNetwork) net).getFlatNeuronList();
            } else {
                toAdd = (ArrayList) ((Network) networkList.get(i)).getNeuronList();
            }

            ret.addAll(toAdd);
        }

        return ret;
    }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses
     *
     * @return the flat list
     */
    public ArrayList getFlatSynapseList() {
        ArrayList ret = new ArrayList();
        ret.addAll(weightList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList toAdd;

            if (net instanceof ComplexNetwork) {
                toAdd = (ArrayList) ((ComplexNetwork) net).getFlatSynapseList();
            } else {
                toAdd = (ArrayList) ((Network) networkList.get(i)).getWeightList();
            }

            ret.addAll(toAdd);
        }

        return ret;
    }

//	public void deleteNeuron(Neuron n) {
//		super.deleteNeuron(n);
//		
//	}
}