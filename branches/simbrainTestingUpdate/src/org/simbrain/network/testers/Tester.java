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
package org.simbrain.network.testers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.groups.Subnetwork;

/**
 * Superclass for all tester classes, which trains a testable object,
 * typically a network.
 *
 *
 * @author jyoshimi
 * @author ztosi
 */
public abstract class Tester {

	/** Listener list. */
    private List<TesterListener> listeners = new ArrayList<TesterListener>();

    /** The testable object to be trained. */
    protected final Testable network;

    //TODO: Generalize to Group instead of Subnetwork?
    /** A reference to the root network. */
    protected final Subnetwork subNet;

    /**
     * Construct the Tester and pass in a reference to the trainable element.
     *
     * @param subNet the sub-network to be tested (included to access custom
     *  update functions).
     * @param network the testable network to be tested.
     */
    public Tester(final Subnetwork subNet, final Testable network) {
        this.subNet = subNet;
    	this.network = network;
    }

    /**
     * Apply the algorithm.
     */
    public abstract void apply();

    /**
     * Add a Tester listener.
     *
     * @param eventListener the listener to add
     */
    public final void addListener(final TesterListener eventListener) {
        if (listeners == null) {
            listeners = new ArrayList<TesterListener>();
        }
        listeners.add(eventListener);
    }

    /**
     * Remove a Tester listener.
     *
     * @param eventListener the listener to add.
     */
    public final void removeListener(final TesterListener eventListener) {
        if (listeners != null) {
            listeners.remove(eventListener);
        }
    }

    /**
     * @return the listeners
     */
    public final List<TesterListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Notify listeners that Testing has begin.
     */
    public final void fireTestingBegin() {
        for (TesterListener listener : getListeners()) {
            listener.beginTesting();
        }
    }

    /**
     * Notify listeners that Testing has ended.
     */
    public final void fireTestingEnd() {
        for (TesterListener listener : getListeners()) {
            listener.endTesting();
        }
    }

    /**
     * Notify listeners of an update in Testing progress. Used by GUI progress
     * bars.
     *
     * @param progressUpdate string description of current state
     * @param percentComplete how far along the Testing is.
     */
    public final void fireProgressUpdate(final String progressUpdate,
    		int percentComplete) {
        for (TesterListener listener : getListeners()) {
            listener.progressUpdated(progressUpdate, percentComplete);
        }
    }

}
