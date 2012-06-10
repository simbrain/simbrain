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
package org.simbrain.network.subnetworks;

import java.util.Collections;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.synapse_update_rules.ClampedSynapseRule;

/**
 * <b>Hopfield</b> is a basic implementation of a discrete Hopfield network.
 */
public class Hopfield extends Subnetwork {

    // TODO: Generalize to capture a greater variety of Hopfield type networks.

    /** Random update. */
    public static final int RANDOM_UPDATE = 1;

    /** Sequential update. */
    public static final int SEQUENTIAL_UPDATE = 0;

    /** Update order. */
    private int updateOrder = SEQUENTIAL_UPDATE;

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_UNITS = 9;

    /** Number of neurons. */
    private int numUnits = DEFAULT_NUM_UNITS;

    /**
     * Copy constructor.
     *
     * @param newRoot new root network
     * @param oldNet old network.
     */
    public Hopfield(Network newRoot, Hopfield oldNet) {
        super(newRoot);
        this.setUpdateOrder(oldNet.getUpdateOrder());
        setLabel("Hopfield network");
    }

    /**
     * Creates a new Hopfield network.
     *
     * @param numNeurons Number of neurons in new network
     * @param layout Neuron layout pattern
     * @param root reference to Network.
     */
    public Hopfield(final Network root, final int numNeurons,
            final Layout layout) {
        super(root, 1, 1);
        setLabel("Hopfield network");

        // Create the neurons
        for (int i = 0; i < numNeurons; i++) {
            BinaryRule binary = new BinaryRule();
            binary.setThreshold(0);
            Neuron n = new Neuron(root, binary);
            n.setUpperBound(1);
            n.setLowerBound(0);
            n.setIncrement(1);
            getNeuronGroup().addNeuron(n);
        }

        // Layout the neurons
        layout.layoutNeurons(this.getNeuronGroup().getNeuronList());

        // Add the synapses
        for (Neuron source : this.getNeuronGroup().getNeuronList()) {
            for (Neuron target : this.getNeuronGroup().getNeuronList()) {
                if (source != target) {
                    Synapse newSynapse = new Synapse(source, target,
                            new ClampedSynapseRule());
                    newSynapse.setStrength(0);
                    getSynapseGroup().addSynapse(newSynapse);
                }
            }
        }

    }

    /**
     * Randomize weights symmetrically.
     */
    public void randomizeWeights() {
        for (int i = 0; i < getNeuronGroup().getNeuronList().size(); i++) {
            for (int j = 0; j < i; j++) {
                Synapse w = Network.getSynapse(getNeuronGroup().getNeuronList()
                        .get(i), getNeuronGroup().getNeuronList().get(j));
                if (w != null) {
                    w.randomize();
                    w.setStrength(Math.round(w.getStrength()));
                }
                Synapse w2 = Network.getSynapse(getNeuronGroup()
                        .getNeuronList().get(j), getNeuronGroup()
                        .getNeuronList().get(i));
                if (w2 != null) {
                    w2.setStrength(w.getStrength());
                }
            }
        }
        getParentNetwork().fireNetworkChanged();
    }

    /**
     * Apply Hopfield training rule to current activation pattern.
     */
    public void train() {
        // Assumes all neurons have the same upper and lower values
        double low = getNeuronGroup().getNeuronList().get(0).getLowerBound();
        double hi = getNeuronGroup().getNeuronList().get(0).getUpperBound();

        for (Synapse w : this.getSynapseGroup().getSynapseList()) {
            Neuron src = w.getSource();
            Neuron tar = w.getTarget();
            w.setStrength(w.getStrength()
                    + ((((2 * src.getActivation()) - hi - low) / (hi - low)) * (((2 * tar
                            .getActivation()) - hi - low) / (hi - low))));
        }
        getParentNetwork().fireNetworkChanged();
    }

    @Override
    public void update() {

        if (getParentNetwork().getClampNeurons()) {
            return;
        }

        int nCount = getNeuronGroup().getNeuronList().size();
        Neuron n;

        if (updateOrder == RANDOM_UPDATE) {
            Collections.shuffle(getNeuronGroup().getNeuronList());
        }

        for (int i = 0; i < nCount; i++) {
            n = getNeuronGroup().getNeuronList().get(i);
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

    public boolean getEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setEnabled(boolean enabled) {
        // TODO Auto-generated method stub

    }

}
