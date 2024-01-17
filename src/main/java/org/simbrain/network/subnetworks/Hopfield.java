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
import org.simbrain.network.core.InfoText;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.simbrain.network.core.NetworkUtilsKt.getEnergy;
import static org.simbrain.util.MathUtilsKt.format;

/**
 * <b>Hopfield</b> is a basic implementation of a discrete Hopfield network.
 */
public class Hopfield extends Subnetwork  {

    /**
     * Custom update rule for Hopfield.
     */
    public static final HopfieldUpdate DEFAULT_UPDATE = HopfieldUpdate.SYNC;

    private NeuronGroup neuronGroup;

    private SynapseGroup weights;

    /**
     * Default number of neurons.
     */
    public static final int DEFAULT_NUM_UNITS = 36;

    /**
     * The update function used by this Hopfield network.
     */
    @UserParameter(label = "Update function")
    private HopfieldUpdate updateFunc = DEFAULT_UPDATE;

    private InfoText infoText;

    /**
     * Training set.
     */
    // private final TrainingSet trainingSet = new TrainingSet();

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
        // values for the Hopfield network creation panel
        if (root == null) {
            return;
        }

        // Create main neuron group
        neuronGroup = new NeuronGroup(root, numNeurons);
        neuronGroup.setLabel("The Neurons");
        neuronGroup.applyLayout();
        addModel(neuronGroup);

        // Set neuron rule
        BinaryRule binary = new BinaryRule();
        binary.setThreshold(0);
        binary.setCeiling(1);
        binary.setFloor(0);
        neuronGroup.setUpdateRule(binary);
        neuronGroup.setIncrement(1);

        // Connect the neurons together
        weights = new SynapseGroup(neuronGroup, neuronGroup);
        weights.setDisplaySynapses(false);
        addModel(weights);

        // Symmetric randomization
        randomize();

        // Create info text
        infoText = new InfoText(getParentNetwork(), getStateInfoText());

    }

    @Override
    public void randomize() {
        getSynapseGroup().randomizeSymmetric();
    }

    @Override
    public void update() {
        updateFunc.update(this);
        updateStateInfoText();
    }

    // @Override
    public NetworkModel getNetwork() {
        return this;
    }

    public NeuronGroup getNeuronGroup() {
        return neuronGroup;
    }

    public SynapseGroup getSynapseGroup() {
        return weights;
    }

    // @Override
    public List<Neuron> getInputNeurons() {
        return this.getFlatNeuronList();
    }

    // @Override
    public List<Neuron> getOutputNeurons() {
        return this.getFlatNeuronList();
    }

    // @Override
    // public TrainingSet getTrainingSet() {
    //     return trainingSet;
    // }

    // @Override
    public void initNetwork() {
        // No implementation
    }

    public String getStateInfoText() {
        return "Energy: " + format(getEnergy(neuronGroup.getNeuronList()), 4);
    }

    public void updateStateInfoText() {
        infoText.setText(getStateInfoText());
        getEvents().getCustomInfoUpdated().fireAndBlock();
    }

    @Override
    public NetworkModel getCustomInfo() {
        return infoText;
    }

    /**
     * Apply the basic Hopfield rule to the current pattern. This is not the
     * main training algorithm, which directly makes use of the input data.
     */
    public void trainOnCurrentPattern() {
        neuronGroup.getNeuronList().forEach(src -> {
            src.getFanIn().forEach(s -> {
                var tar = s.getSource();
                var deltaW = bipolar(src.getActivation()) * bipolar(tar.getActivation());
                s.setStrength(s.getStrength() + deltaW);
            });
        });
        getEvents().getUpdated().fireAndForget();
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

    /**
     * Main forms of Hopfield update rule.
     */
    public enum HopfieldUpdate {
        RAND {
            /**
             * Update neurons in random order
             */
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
            public String toString() {
                return "Random";
            }

        }, SEQ {
            /**
             * Sequential update of neurons (same sequence every time)
             */
            @Override
            public void update(Hopfield hop) {
                // TODO: Cache the sorted list
                hop.neuronGroup.getNeuronList()
                        .stream().sorted(Comparator.comparing(Neuron::getUpdatePriority))
                        .forEach(n -> {
                            n.updateInputs();
                            n.update();
                        });
            }


            @Override
            public String toString() {
                return "Sequential";
            }

        }, SYNC {
            @Override
            public void update(Hopfield hop) {
                hop.getNeuronGroup().getNeuronList().forEach(Neuron::updateInputs);
                hop.getNeuronGroup().getNeuronList().forEach(Neuron::update);
            }

            @Override
            public String toString() {
                return "Synchronous";
            }

        };


        public abstract void update(Hopfield hop);

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
