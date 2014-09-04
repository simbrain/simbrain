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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;

/**
 * Superclass for all trainer classes, which trains a trainable object,
 * typically a network.
 *
 * @author jeffyoshimi
 *
 */
public abstract class Trainer {

    /** Listener list. */
    private List<TrainerListener> listeners = new ArrayList<TrainerListener>();

    /** The trainable object to be trained. */
    protected final Trainable network;

    /**
     * Construct the trainer and pass in a reference to the trainable element.
     *
     * @param network
     *            the network to be trained
     */
    public Trainer(Trainable network) {
        this.network = network;
    }

    /**
     * Apply the algorithm.
     *
     * @throws DataNotInitializedException
     *             when input or target data have not been set.
     */
    public abstract void apply() throws DataNotInitializedException;

    /**
     * Add a trainer listener.
     *
     * @param trainerListener
     *            the listener to add
     */
    public void addListener(final TrainerListener trainerListener) {
        if (listeners == null) {
            listeners = new ArrayList<TrainerListener>();
        }
        listeners.add(trainerListener);
    }

    /**
     * Remove a trainer listener.
     *
     * @param trainerListener
     *            the listener to add
     */
    public void removeListener(final TrainerListener trainerListener) {
        if (listeners != null) {
            listeners.remove(trainerListener);
        }
    }

    /**
     * @return the listeners
     */
    public List<TrainerListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Notify listeners that training has begin.
     */
    public void fireTrainingBegin() {
        for (TrainerListener listener : getListeners()) {
            listener.beginTraining();
        }
    }

    /**
     * Notify listeners that training has ended.
     */
    public void fireTrainingEnd() {
        for (TrainerListener listener : getListeners()) {
            listener.endTraining();
        }
    }

    /**
     * Notify listeners of an update in training progress. Used by GUI progress
     * bars.
     *
     * @param progressUpdate
     *            string description of current state
     * @param percentComplete
     *            how far along the training is.
     */
    public void fireProgressUpdate(String progressUpdate, int percentComplete) {
        for (TrainerListener listener : getListeners()) {
            listener.progressUpdated(progressUpdate, percentComplete);
        }
    }

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
     *
     */
    public class DataNotInitializedException extends Exception {

        /**
         * @param message
         *            error message
         */
        public DataNotInitializedException(final String message) {
            super(message);
        }

    }

    /**
     * Helper function to update synapse groups whose synapses may have changed
     * based on training.
     */
    public void revalidateSynapseGroups() {
        if (getTrainableNetwork().getNetwork() instanceof Subnetwork) {
            for (SynapseGroup group : ((Subnetwork) getTrainableNetwork()
                    .getNetwork()).getSynapseGroupList()) {
                if (group != null) {
                    group.revalidateSynapseSets();
                }
            }
        }
    }

    /**
     * Utility method for creating a trainable object.
     *
     * @param trainedGroup the subnet or synapse group being trained.
     * @param inputNeurons the input neurons
     * @param outputNeurons the output neurons
     * @param inputData the input data
     * @param targetData the target data
     * @return the trainable object
     */
    public static Trainable getTrainable(final Group trainedGroup, final List<Neuron> inputNeurons,
            final List<Neuron> outputNeurons, final double[][] inputData,
            final double[][] targetData) {
        Trainable newTrainer = new Trainable() {
            public Group getNetwork() {
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

}
