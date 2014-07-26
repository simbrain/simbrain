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
import java.util.HashSet;
import java.util.List;

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

    public enum HopfieldUpdate {
        RAND {

            @Override
            public void update(Hopfield hop) {
                List<Neuron> neurons = hop.getModifiableNeuronList();
                Neuron neuron = null;
                Collections.shuffle(neurons);
                for (int i = 0, n = neurons.size(); i < n; i++) {
                    neuron = neurons.get(i);
                    neuron.update();
                    neuron.setActivation(neuron.getBuffer());
                }
            }

            @Override
            public String getDescription() {
                return "Randomly ordered sequential update (different every"
                        + " time)";
            }

            @Override
            public String getName() {
                return "Random";
            }

        },
        SEQ {

            @Override
            public void update(Hopfield hop) {
                List<Neuron> neurons;
                if (hop.isByPriority()) {
                    neurons = hop.getParentNetwork()
                            .getPrioritySortedNeuronList();
                    for (Neuron n : neurons) {
                        // TODO: Hack to allow hopfield networks to be updated
                        // within based on priority, without having to sort
                        // the list every iteration.
                        if (hop.getNeuronSet().contains(n)) {
                            n.update();
                            n.setActivation(n.getBuffer());
                        }
                    }
                } else {
                    neurons = hop.getFlatNeuronList();
                    for (Neuron n : neurons) {
                        n.update();
                        n.setActivation(n.getBuffer());
                    }
                }

            }

            @Override
            public String getDescription() {
                return "Sequential update of neurons (same seqence every time)";
            }

            @Override
            public String getName() {
                return "Sequential";
            }

        },
        SYNC {

            @Override
            public void update(Hopfield hop) {
                List<Neuron> neurons = hop.getFlatNeuronList();
                for (Neuron n : neurons) {
                    n.update();
                }
                for (Neuron n : neurons) {
                    n.setActivation(n.getBuffer());
                }
            }

            @Override
            public String getDescription() {
                return "Synchronous update of neurons";
            }

            @Override
            public String getName() {
                return "Synchronous";
            }

        };

        public static HopfieldUpdate getUpdateFuncFromName(String name) {
            for (HopfieldUpdate hu : HopfieldUpdate.values()) {
                if (name.equals(hu.getName())) {
                    return hu;
                }
            }
            throw new IllegalArgumentException("No such Hopfield update"
                    + "function");
        }

        public static String[] getUpdateFuncNames() {
            String[] names = new String[HopfieldUpdate.values().length];
            for (int i = 0; i < HopfieldUpdate.values().length; i++) {
                names[i] = HopfieldUpdate.values()[i].getName();
            }
            return names;
        }

        public abstract void update(Hopfield hop);

        public abstract String getDescription();

        public abstract String getName();
    }

    /** Default update mechanism. */
    public static final HopfieldUpdate DEFAULT_UPDATE = HopfieldUpdate.SEQ;

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_UNITS = 9;

    /** Number of neurons. */
    private int numUnits = DEFAULT_NUM_UNITS;

    /** The update function used by this Hopfield network. */
    private HopfieldUpdate updateFunc = DEFAULT_UPDATE;

    /**
     * If true, if the network's update order is sequential, it will update in
     * order of priority.
     */
    private boolean byPriority = false;

    /**
     * The set of neurons... here as a hack while the priority update within
     * groups issue is being resolved.
     */
    private HashSet<Neuron> neuronSet = new HashSet<Neuron>();

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

    // /**
    // * Copy constructor.
    // *
    // * @param newRoot new root network
    // * @param oldNet old network.
    // */
    // public Hopfield(Network newRoot, Hopfield oldNet) {
    // super(newRoot);
    // setUpdateFunc(oldNet.getUpdateFunc());
    // setLabel("Hopfield network");
    // }

    /**
     * Creates a new Hopfield network.
     *
     * @param numNeurons
     *            Number of neurons in new network
     * @param root
     *            reference to Network.
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
            binary.setFloor(-1);
            binary.setIncrement(1);
            Neuron n = new Neuron(root, binary);
            getNeuronGroup().addNeuron(n);
            neuronSet.add(n);
        }

        // Add the synapses
        for (Neuron source : this.getNeuronGroup().getNeuronList()) {
            for (Neuron target : this.getNeuronGroup().getNeuronList()) {
                if (source != target) {
                    Synapse newSynapse = new Synapse(source, target,
                            new StaticSynapseRule());
                    newSynapse.setStrength(0);
                    getSynapseGroup().addSynapseUnsafe(newSynapse);
                }
            }
        }

        getSynapseGroup().initializeSynapseVisibility();

    }

    /**
     * Randomizes the update sequence by shuffling the neuron list associated
     * with this Hopfield network. Only has an effect if the update function is
     * sequential.
     */
    public void randomizeSequence() {
        Collections.shuffle(this.getModifiableNeuronList());
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
        updateFunc.update(this);
    }

    /**
     * @return The number of neurons.
     */
    public int getNumUnits() {
        return numUnits;
    }

    @Override
    public String getUpdateMethodDesecription() {
        return updateFunc.getDescription();
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
        for (Synapse w : this.getSynapseGroup().getAllSynapses()) {
            Neuron src = w.getSource();
            Neuron tar = w.getTarget();
            getSynapseGroup().setSynapseStrength(w,
                    w.getStrength() + src.getActivation()
                    * tar.getActivation());
        }
        getParentNetwork().fireNetworkChanged();
    }

    /**
     * @return the updateFunc
     */
    public HopfieldUpdate getUpdateFunc() {
        return updateFunc;
    }

    /**
     * @param updateFunc
     *            the updateFunc to set
     */
    public void setUpdateFunc(HopfieldUpdate updateFunc) {
        this.updateFunc = updateFunc;
    }

    /**
     * @return the neuronSet
     */
    public HashSet<Neuron> getNeuronSet() {
        return neuronSet;
    }

    /**
     * @param neuronSet
     *            the neuronSet to set
     */
    public void setNeuronSet(HashSet<Neuron> neuronSet) {
        this.neuronSet = neuronSet;
    }

    public boolean isByPriority() {
        return byPriority;
    }

    public void setByPriority(boolean byPriority) {
        this.byPriority = byPriority;
    }

}
