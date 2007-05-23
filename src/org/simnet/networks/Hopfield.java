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
package org.simnet.networks;

import java.util.Collections;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.RootNetwork;
import org.simnet.interfaces.Synapse;
import org.simnet.layouts.Layout;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.synapses.ClampedSynapse;


/**
 * <b>Hopfield</b>.
 */
public class Hopfield extends Network {

    /** Random update. */
    public static final int RANDOM_UPDATE = 1;

    /** Sequential update. */
    public static final int SEQUENTIAL_UPDATE = 0;

    /** Update order. */
    private int updateOrder = SEQUENTIAL_UPDATE;

    /** Number of neurons. */
    private int numUnits = 9;

    /**
     * Default constructor.
     */
    public Hopfield() {
        super();
    }

    /**
     * Creates a new hopfield network.
     * @param numNeurons Number of neurons in new network
     * @param layout Neuron layout patern
     * @param root reference to RootNetwork.
     */
    public Hopfield(final RootNetwork root, final int numNeurons, final Layout layout) {
        super();
        this.setRootNetwork(root);
        this.setParentNetwork(root);

        //Create the neurons
        for (int i = 0; i < numNeurons; i++) {
            BinaryNeuron n = new BinaryNeuron();
            n.setUpperBound(1);
            n.setLowerBound(-1);
            n.setThreshold(0);
            n.setIncrement(1);
            addNeuron(n);
        }
        layout.layoutNeurons(this);
        createConnections();
    }


    /**
     * Create full symmetric connections without self-connections.
     */
    public void createConnections() {
        for (int i = 0; i < this.getNeuronCount(); i++) {
            for (int j = 0; j < i; j++) {
                ClampedSynapse w = new ClampedSynapse();
                w.setSource(this.getNeuron(i));
                w.setTarget(this.getNeuron(j));
                w.setUpperBound(1);
                w.setLowerBound(-1);
                w.randomize();
                w.setStrength(Network.round(w.getStrength(), 0));
                addSynapse(w);

                ClampedSynapse w2 = new ClampedSynapse();
                w2.setSource(this.getNeuron(j));
                w2.setTarget(this.getNeuron(i));
                w2.setUpperBound(1);
                w2.setLowerBound(-1);
                w2.setStrength(w.getStrength());
                addSynapse(w2);
            }
        }
    }

    /**
     * Randomize weights symmetrically.
     */
    public void randomizeWeights() {
        for (int i = 0; i < getNeuronCount(); i++) {
            for (int j = 0; j < i; j++) {
                Synapse w = Network.getSynapse(getNeuron(i), getNeuron(j));
                w.randomize();
                w.setStrength(Network.round(w.getStrength(), 0));

                Synapse w2 = Network.getSynapse(getNeuron(j), getNeuron(i));
                w2.setStrength(w.getStrength());
            }
        }
        getRootNetwork().fireNetworkChanged();
    }

    /**
     * Apply hopfield training rule to current activation pattern.
     */
    public void train() {
        //Assumes all neurons have the same upper and lower values
        double low = getNeuron(0).getLowerBound();
        double hi = getNeuron(0).getUpperBound();

        for (int i = 0; i < this.getSynapseCount(); i++) {
            //Must use buffer
            Synapse w = this.getSynapse(i);
            Neuron src = w.getSource();
            Neuron tar = w.getTarget();
            w.setStrength(w.getStrength()
                          + ((((2 * src.getActivation()) - hi - low) / (hi - low)) * (((2 * tar.getActivation()) - hi
                          - low) / (hi - low))));
        }
        getRootNetwork().fireNetworkChanged();
    }

    /**
     * Update nodes randomly or sequentially.
     */
    public void update() {

        if (getRootNetwork().getClampNeurons()) {
            return;
        }

        int nCount = getNeuronCount();
        Neuron n;

        if (updateOrder == RANDOM_UPDATE) {
            Collections.shuffle(getNeuronList());
        }

        for (int i = 0; i < nCount; i++) {
            n = (Neuron) getNeuronList().get(i);
            n.update();
            n.setActivation(n.getBuffer());
        }
    }

    /**
     * @return The number of neurons.
     */
    public int getNumUnits() {
        return numUnits;
    }

    /**
     * @return The update order.
     */
    public int getUpdateOrder() {
        return updateOrder;
    }

    /**
     * Sets the update order.
     *
     * @param updateOrder The value to set
     */
    public void setUpdateOrder(final int updateOrder) {
        this.updateOrder = updateOrder;
    }

    @Override
    public Network duplicate() {
        Hopfield net = new Hopfield();
        net = (Hopfield) super.duplicate(net);
        return net;
    }
}
