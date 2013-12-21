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

import java.util.List;
import java.util.Random;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;

/**
 * <b>Hopfield</b> is a basic implementation of a discrete Hopfield network.
 */
public class Hopfield extends Subnetwork implements Trainable {

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

    /** Random integer generator. */
    private Random randInt;

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

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
     * @param root reference to Network.
     */
    public Hopfield(final Network root, final int numNeurons) {
        super(root);
        setLabel("Hopfield network");
        NeuronGroup hopfieldGroup = new NeuronGroup(root);
        this.addNeuronGroup(hopfieldGroup);
        this.connectNeuronGroups(hopfieldGroup, hopfieldGroup);

        this.setDisplayNeuronGroups(false);

        // Create the neurons
        for (int i = 0; i < numNeurons; i++) {
            BinaryRule binary = new BinaryRule();
            binary.setThreshold(0);
            binary.setCeiling(1);
            binary.setFloor(0);
            binary.setIncrement(1);
            Neuron n = new Neuron(root, binary);
            getNeuronGroup().addNeuron(n);
        }

        // Add the synapses
        for (Neuron source : this.getNeuronGroup().getNeuronList()) {
            for (Neuron target : this.getNeuronGroup().getNeuronList()) {
                if (source != target) {
                    Synapse newSynapse = new Synapse(source, target,
                            new StaticSynapseRule());
                    newSynapse.setStrength(0);
                    getSynapseGroup().addSynapse(newSynapse);
                }
            }
        }

    }

    /**
     * Randomize weights symmetrically.
     */
    public void randomize() {
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

    @Override
    public void update() {

        int nCount = getNeuronGroup().getNeuronList().size();
        Neuron n;

        if (updateOrder == RANDOM_UPDATE) {
            if (randInt == null) {
                randInt = new Random();
            }
            for (int i = 0; i < nCount; i++) {
                n = getNeuronGroup().getNeuronList().get(
                        randInt.nextInt(nCount));
                n.update();
                n.setActivation(n.getBuffer());
            }
        } else {
            getNeuronGroup().update();
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
    public String getUpdateMethodDesecription() {
        if (updateOrder == RANDOM_UPDATE) {
            return "Random update of neurons";
        } else {
            return "Sequential update of neurons";
        }
    }

    @Override
    public List<Neuron> getInputNeurons() {
        return this.getFlatNeuronList();
    }

    @Override
    public List<Neuron> getOutputNeurons() {
        return this.getFlatNeuronList();
    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    @Override
    public void initNetwork() {
        // No implementation
    }

    /**
     * Apply the basic Hopfield rule to the current pattern. This is not the
     * main training algorithm, which directly makes use of the input data.
     */
    public void trainOnCurrentPattern() {
        for (Synapse w : this.getSynapseGroup().getSynapseList()) {
            Neuron src = w.getSource();
            Neuron tar = w.getTarget();
            w.setStrength(w.getStrength() + src.getActivation()
                    * tar.getActivation());
        }
        getParentNetwork().fireNetworkChanged();
    }

}
