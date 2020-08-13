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
package org.simbrain.network.trainers;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.events.NetworkEvents;
import org.simbrain.network.events.TrainerEvents;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.propertyeditor.EditableObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for all trainer classes, which trains a trainable object,
 * typically a network.
 *
 * @author jeffyoshimi
 */
public abstract class Trainer implements EditableObject {

    /**
     * Listener list.
     */
    private List<TrainerListener> listeners = new ArrayList<TrainerListener>();

    /**
     * The trainable object to be trained.
     * //TODO!
     */
    protected Trainable network;

    /**
     * Handle trainer events.
     */
    private transient TrainerEvents events = new TrainerEvents(this);

    /**
     * Construct the trainer and pass in a reference to the trainable element.
     *
     * @param network the network to be trained
     */
    public Trainer(Trainable network) {
        this.network = network;
    }

    public Trainer() {
    }

    /**
     * Apply the algorithm.
     *
     * @throws DataNotInitializedException when input or target data have not been set.
     */
    public abstract void apply() throws DataNotInitializedException;

    /**
     * @return the network
     */
    public Trainable getTrainableNetwork() {
        return network;
    }

    /**
     * Exception thrown when a training algorithm is applied but no data have
     * been initialized.
     *
     * @author jyoshimi
     */
    public class DataNotInitializedException extends Exception {

        /**
         * @param message error message
         */
        public DataNotInitializedException(final String message) {
            super(message);
        }

    }

    /**
     * Helper function to update synapse groups whose synapses may have changed
     * based on training.
     */
    // Most trainers should now use dl4j weight matrices
    @Deprecated()
    public void revalidateSynapseGroups() {
        if (getTrainableNetwork().getNetwork() instanceof Subnetwork) {
            for (SynapseGroup sg : ((Subnetwork) getTrainableNetwork().getNetwork()).getSynapseGroupList()) {
                if (sg != null) {
                    sg.revalidateSynapseSets();
                }
            }
        }
    }

    /**
     * Utility method for creating a trainable object.
     *
     * @param trainedGroup  the subnet or synapse group being trained.
     * @param inputNeurons  the input neurons
     * @param outputNeurons the output neurons
     * @param inputData     the input data
     * @param targetData    the target data
     * @return the trainable object
     */
    public static Trainable getTrainable(final NeuronGroup trainedGroup, final List<Neuron> inputNeurons, final List<Neuron> outputNeurons, final double[][] inputData, final double[][] targetData) {
        Trainable newTrainer = new Trainable() {
            public NeuronGroup getNetwork() {
                return trainedGroup;
            }

            public List<Neuron> getInputNeurons() {
                return inputNeurons;
            }

            public List<Neuron> getOutputNeurons() {
                return outputNeurons;
            }

            public TrainingSet getTrainingSet() {
                return new TrainingSet(inputData, targetData);
            }

            public void initNetwork() {
            }
        };
        return newTrainer;
    }

    public TrainerEvents getEvents() {
        return events;
    }
}
