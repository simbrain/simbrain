/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.trainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for all types of trainer which can be iterated and which return an
 * error when they are iterated.
 * 
 * @author jyoshimi
 */
public abstract class IterableTrainer extends Trainer {

    /** Flag used for iterative training methods. */
    private boolean updateCompleted = true;
    
    /** Listener list. */
    private List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();

    /** Iteration number. */
    private int iteration;
   
    /**
     * Construct the iterable trainer.
     *
     * @param network the trainable network
     */
    public IterableTrainer(Trainable network) {
    	super(network);
    }

    /**
     * Get the current error.
     *
     * @return the current error
     */
    public abstract double getError();
    
    /**
     * Iterate the trainer for a set number of iterations
     * 
     * @param iterations
     */
    public void iterate(int iterations) {
    	fireTrainingBegin();
    	for (int i = 0; i < iterations; i++) {
    		apply();
    	}
    	fireTrainingEnd();
    }
        
    /**
     * Iterate the trainer until it is below a threshold.
     * NOTE: Not yet used or tested.
     * 
     * @param iterations
     */
    public void iterateBelowThreshold(double threshold) {
    	fireTrainingBegin();
    	// TODO: Need some way of escaping... manually via a stop
    	// button or through a pre-set max iterations
    	while (getError() > threshold) {
    		apply();
    	}
    	fireTrainingEnd();
    }
    
    /**
     * Notify listeners that the error value has been updated. Only makes sense
     * for iterable methods.
     */
	public void fireErrorUpdated() {
		for (ErrorListener listener : getErrorListeners()) {
			((ErrorListener) listener).errorUpdated();
		}
	}
    
    /**
     * @return boolean updated completed.
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    public void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }
    
    /**
     * Increment the iteration number by 1. 
     */
    public void incrementIteration() {
    	iteration++;
    }

	/**
	 * @param iteration the iteration to set
	 */
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	/**
	 * Return the current iteration.
	 *
	 * @return current iteration.
	 */
    public int getIteration() {
        return iteration;
    }

	/**
	 * @return the errorListener
	 */
	public List<ErrorListener> getErrorListeners() {
		return Collections.unmodifiableList(errorListeners);
	}

	/**
	 * Add an error listener.
	 *
	 * @param errorListener the listener to add
	 */
	public void addErrorListener(final ErrorListener errorListener) {
        if (errorListeners == null) {
        	errorListeners = new ArrayList<ErrorListener>();
        }
        errorListeners.add(errorListener);	}

	/**
	 * Remove an error listener.
	 *
	 * @param errorListener the listener to remove
	 */
	public void removeErrorListener(final ErrorListener errorListener) {
        if (errorListeners != null) {
    		errorListeners.remove(errorListener);	        	
        }
	}

}
