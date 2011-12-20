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
package org.simbrain.network.networks;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.LinearNeuron;

/**
 * <b>StandardNetwork</b> serves as a high-level container for other networks
 * and neurons. It contains a list of neurons as well as a list of networks.
 * When building simulations in which multiple networks interact, this should be
 * the top-level network which contains the rest.
 *
 * @author yoshimi
 */
public class Standard extends Network {

    /** Initial number of neurons. */
    private int numNeurons = 5;

    /**
     * Default constructor.
     */
    public Standard() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param newRoot new root network
     * @param oldNet old network.
     */
    public  Standard(RootNetwork newRoot,  Standard oldNet) {
        super(newRoot, oldNet);
    }

    /**
     * Construct with root network.
     *
     * @param root root network.
     */
    public Standard(RootNetwork root) {
        setRootNetwork(root);
    }

    /**
     * Construct a Standard Network with a specified number of units.
     *
     * @param nUnits how many units this network should have.
     * @param layout how the units should be layed out.
     * @param root reference to RootNetwork.
     */
    public Standard(final RootNetwork root, final int nUnits,
            final Layout layout) {
        super();
        this.setRootNetwork(root);

        numNeurons = nUnits;
        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new Neuron(this, new LinearNeuron()));
        }
        layout.layoutNeurons(this);
    }

    /**
     * The core update function of the neural network. Calls the current update
     * function on each neuron, decays all the neurons, and checks their bounds.
     */
    public void update() {
        updateAllNeurons();
        updateAllSynapses();
    }

    /**
     * Set delays on all synapses to this network.
     *
     * TODO: Move to network?
     *
     * @param newDelay the delay to set.
     */
    public void setDelays(final int newDelay) {
        for (int i = 0; i < this.getNeuronCount(); i++) {
            for (Synapse syn : this.getNeuron(i).getFanIn()) {
                syn.setDelay(newDelay);
            }
        }
    }

    /**
     * Returns the initial number of neurons.
     *
     * @return the initial number of neurons
     */
    public int getNumNeurons() {
        return numNeurons;
    }

}
