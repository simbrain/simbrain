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

import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>Hopfield</b> is a basic implementation of a discrete Hopfield network.
 */
public class Hopfield extends Subnetwork implements Trainable {

    /**
     * Default update mechanism.
     */
    public static final HopfieldUpdate DEFAULT_UPDATE = HopfieldUpdate.SYNC;

    private WeightMatrix wm;

    private NeuronGroup neuronGroup;

    /**
     * Default number of neurons.
     */
    public static final int DEFAULT_NUM_UNITS = 36;

    /**
     * The update function used by this Hopfield network.
     */
    public static final boolean DEFAULT_PRIORITY = false;

    /**
     * The update function used by this Hopfield network.
     */
    @UserParameter(label = "Update function")
    private HopfieldUpdate updateFunc = DEFAULT_UPDATE;

    /**
     * If true, if the network's update order is sequential, it will update in
     * order of priority.
     */
    @UserParameter(label = "Sequential (vs. Random) Update")
    private boolean byPriority = DEFAULT_PRIORITY;

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

    /**
     * Creates a new Hopfield network.
     *
     * @param numNeurons Number of neurons in new network
     * @param root       reference to Network.
     */
    public Hopfield(final Network root, final int numNeurons) {
        super(root);
        setLabel("Hopfield network");

        // In this case the network object is being used by to store default
        // values for the hopfield network creation panel
        if (root == null) {
            return;
        }

        // Create main neuron group
        neuronGroup = new NeuronGroup(root, numNeurons);
        neuronGroup.setLabel("The Neurons");
        addModel(neuronGroup);

        // Set neuron rule
        BinaryRule binary = new BinaryRule();
        binary.setThreshold(0);
        binary.setCeiling(1);
        binary.setFloor(0);
        neuronGroup.setNeuronType(binary);
        neuronGroup.setIncrement(1);

        // Connect the neurons together
        wm = new WeightMatrix(getParentNetwork(), neuronGroup, neuronGroup);
        addModel(wm);

    }

    @Override
    public void randomize() {
        wm.randomize();
    }

    @Override
    public void update() {
        updateFunc.update(this);
    }

    @Override
    public NetworkModel getNetwork() {
        return this;
    }

    public NeuronGroup getNeuronGroup() {
        return neuronGroup;
    }

    public WeightMatrix getWeightMatrix() {
        return wm;
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
        // TODO
        // for (Synapse w : this.getSynapseGroup().getAllSynapses()) {
        //     Neuron src = w.getSource();
        //     Neuron tar = w.getTarget();
        //     getSynapseGroup().setSynapseStrength(w, w.getStrength() + bipolar(src.getActivation()) * bipolar(tar.getActivation()));
        // }
        //getParentNetwork().fireGroupUpdated(getSynapseGroup());
    }

    /**
     * Convenience method to convert binary values (1,0) to bipolar
     * values(1,-1).
     *
     * @param in number to convert
     * @return converted number
     */
    public static double bipolar(double in) {
        return in == 0 ? -1 : in;
    }

    public HopfieldUpdate getUpdateFunc() {
        return updateFunc;
    }

    public void setUpdateFunc(HopfieldUpdate updateFunc) {
        this.updateFunc = updateFunc;
    }

    public boolean isByPriority() {
        return byPriority;
    }

    public void setByPriority(boolean byPriority) {
        this.byPriority = byPriority;
    }

    /**
     * Main forms of Hopfield update rule.
     */
    public enum HopfieldUpdate {
        RAND {
            @Override
            public void update(Hopfield hop) {
                List<Neuron> copy = new ArrayList<>(hop.getNeuronGroup().getNeuronList());
                Collections.shuffle(copy);
                copy.forEach(n -> {
                    n.updateInputs();
                    n.update();
                });
            }

            @Override
            public String getDescription() {
                return "Randomly ordered sequential update (different every" + " time)";
            }

            @Override
            public String getName() {
                return "Random";
            }

        }, SEQ {
            @Override
            public void update(Hopfield hop) {
                List<Neuron> neurons;
                if (hop.isByPriority()) {
                    List<Neuron> copy = new ArrayList<>(hop.getNeuronGroup().getNeuronList());
                    // TODO: Sort by priority
                    copy.forEach(n -> {
                        n.updateInputs();
                        n.update();
                    });
                } else {
                    hop.getNeuronGroup().getNeuronList().forEach(n -> {
                        n.updateInputs();
                        n.update();
                    });
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

        }, SYNC {
            @Override
            public void update(Hopfield hop) {
                List<Neuron> neurons = hop.getFlatNeuronList();
                for (Neuron n : neurons) {
                    n.update();
                }
                for (Neuron n : neurons) {
                    n.setActivation(n.getActivation());
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
            throw new IllegalArgumentException("No such Hopfield update" + "function");
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

    /**
     * Helper class for creating new Hopfield nets using {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class HopfieldCreator implements EditableObject {

        @UserParameter(label = "Number of neurons", description = "How many neurons this Hofield net should have", order = -1)
        int numNeurons = DEFAULT_NUM_UNITS;

        /**
         * Create the hopfield net
         */
        public Hopfield create(Network network) {
            return new Hopfield(network, numNeurons);
        }

    }

}
