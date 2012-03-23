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

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Synapse;

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
     * @param network the network to be trained
     */
    public Trainer(Trainable network) {
    	this.network = network;
    }
    
    /**
     * Apply the algorithm.
     */
    public abstract void apply();
    
    /**
     * Add a trainer listener.
     *
     * @param trainerListener the listener to add
     */
    public void addListener(final TrainerListener eventListener) {
        if (listeners == null) {
            listeners = new ArrayList<TrainerListener>();
        }
        listeners.add(eventListener);
    }
    
    /**
     * Remove a trainer listener.
     *
     * @param trainerListener the listener to add
     */
    public void removeListener(final TrainerListener eventListener) {
        if (listeners != null) {
            listeners.remove(eventListener);
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
	 * Notify listeners of an update in training progress.  Used by GUI progress bars.
	 *
	 * @param progressUpdate string description of current state
	 * @param percentComplete how far along the training is.
	 */
	public void fireProgressUpdate (String progressUpdate, int percentComplete) {
    	for(TrainerListener listener : getListeners()){
    		listener.progressUpdated(progressUpdate, percentComplete);
    	}
    }
	
	/**
	 * Connect the input neurons to the output neurons using a specified weight matrix, and
	 * add those to the specified synapse groups.
	 *
	 * @param w the matrix of weights to set
	 * @param group the synapse group to add weights to
	 */
	public void connectInputOutput(final double[][] w, SynapseGroup group) {
		for (int i = 0; i < network.getInputNeurons().size(); i++) {
			for (int j = 0; j < network.getOutputNeurons().size(); j++) {
				Synapse s = Network.getSynapse(network.getInputNeurons().get(i),
						network.getOutputNeurons().get(j));
				if (s != null) {
					s.setStrength(w[i][j]);
				} else {
					Synapse newSynapse = new Synapse(network.getInputNeurons().get(i),
							network.getOutputNeurons().get(j));
					newSynapse.setStrength(w[i][j]);
					group.addSynapse(newSynapse);
				}
			}
		}
	}

}
