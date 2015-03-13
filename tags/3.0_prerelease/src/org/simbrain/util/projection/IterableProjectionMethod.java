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
package org.simbrain.util.projection;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.trainers.ErrorListener;

/**
 * <b>IterableProjectionMethod</b> extends projection method and provides tools
 * for iterating a projection method.
 */
public abstract class IterableProjectionMethod extends ProjectionMethod {

    /** Listener list. */
    private List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();

    /** The current error. */
    private double error;

    /**
     * Set to true when the iterative algorithm needs to be re-initialized (e.g
     * after a new datapoint is added to the upstairs dataset.
     */
    private boolean needsReInit;

    /**
     * Construct the projection method.
     *
     * @param projector reference to parent projector.
     */
    public IterableProjectionMethod(Projector projector) {
        super(projector);
    }

    /**
     * Notify listeners that the error value has been updated. Only makes sense
     * for iterable methods.
     */
    public void fireErrorUpdated() {
        for (ErrorListener listener : errorListeners) {
            listener.errorUpdated();
        }
    }

    /**
     * Iterate, if it is iterable. For non iterable-projectors this is just a
     * stub.
     */
    public abstract void iterate();

    /**
     * Returns the current error associated with this projection method.
     *
     * @return current error
     */
    public double getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(double error) {
        this.error = error;
    }

    /**
     * @return the needsReInit
     */
    public boolean needsReInit() {
        return needsReInit;
    }

    /**
     * @param needsReInit the needsReInit to set
     */
    public void setNeedsReInit(boolean needsReInit) {
        this.needsReInit = needsReInit;
    }
}
