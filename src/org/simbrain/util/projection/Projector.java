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

import org.apache.log4j.Logger;

/**
 * <b>Projector</b> is the main class of the high dimensional visualizer, which  provides methods for changing and
 * initializing various projection algorithms.
 * 
 * TODO:
 * - Is error needed here?
 * - Changemore "projector" to "projectionmethod"
 * 
 */
public class Projector {

    /** Log4j logger. */
    private static final Logger logger = Logger.getLogger(Projector.class);

    /** Reference to object containing projection settings. */
    private Settings projectorSettings = new Settings();

    /** References to projection objects.*/
    private ProjectionMethod currentProjectionMethod;

    /** How the datasets will be displayed. */
    private String defaultProjectionMethod = GaugePreferences.getDefaultProjector();

    /** Application parameters. */
    private double error = 0;

    /** Current data point.  */
    double[] currentState = null;

    /** List of available projection algorithms. */
    public static final String[] PROJECTION_METHOD_LIST = {
            "Sammon", "PCA", "Coordinate" };

    /**
     * Default constructor for gauge.
     */
    public Projector() {
        currentProjectionMethod = this.getProjectionMethodByName(defaultProjectionMethod);
    }

    /**
     * Initialize gauge to accept data of a specified dimension.
     *
     * @param dims dimensionality of the high dimensional dataset
     */
    public void init(final int dims) {
        currentProjectionMethod.init(dims);
        currentState = org.simbrain.util.SimbrainMath.zeroVector(dims);
    }

    /**
     * Add a new point to the dataset, using the currently selected add method.
     *
     * @param point the point to add
     */
    public boolean addDatapoint(final double[] point) {

        //TODO: Throw exception if point does not match
        
        logger.debug("addDatapoint called");
        if ((currentProjectionMethod == null) || (getUpstairs() == null)) {
            return false;
        }

        boolean ret = currentProjectionMethod.addDatapoint(point);

        /* This is needed to invoke the current projector's init function */
        if (currentProjectionMethod.isIterable()) {
            currentProjectionMethod.init(getUpstairs(), getDownstairs());
        }

        error = 0;
        return ret;
    }

    /**
     * Iterate the dataset some fixed number of times.
     *
     * @param numTimes Number of times to iterate the gauge
     */
    public void iterate(final int numTimes) {
        if (!currentProjectionMethod.isIterable()) {
            return;
        }

        int iterations = 0;

        while (iterations < numTimes) {
        	// TODO: Why should the current projector return an error when that is specific to Sammon? 
            error = currentProjectionMethod.iterate();
            iterations++;
        }
    }

    /**
     * @return list of projector types, by name
     */
    public static String[] getProjectorList() {
        return PROJECTION_METHOD_LIST;
    }

    /**
     * @param projName the name of the projection algorithm to switch to
     */
    public void setCurrentProjectionMethod(final String projName) {
        if (projName == null) {
            return;
        }
        ProjectionMethod newProjector = getProjectionMethodByName(projName);
        newProjector.init(currentProjectionMethod.getUpstairs(), currentProjectionMethod.getDownstairs());
        setCurrentProjectionMethod(newProjector);
    }

    /**
     * @param name name of projector
     * @return ProjectionMethod type by name.
     */
    public ProjectionMethod getProjectionMethodByName(final String name) {
        ProjectionMethod ret = null;

        if (name.equalsIgnoreCase("Sammon")) {
            ret = new ProjectSammon(projectorSettings);
        } else if (name.equalsIgnoreCase("Coordinate")) {
            ret = new ProjectCoordinate(projectorSettings);
        } else if (name.equalsIgnoreCase("PCA")) {
            ret = new ProjectPCA(projectorSettings);
        }

        return ret;
    }

    /**
     * Number of dimensions of the underlying data.
     *
     * @return dimensions of the underlying data
     */
    public int getDimensions() {
        if (currentProjectionMethod == null) {
            return 0;
        } else if (currentProjectionMethod.getUpstairs() == null) {
            return 0;
        }
        return currentProjectionMethod.getUpstairs().getDimensions();
    }

    /**
     * @param newProj the new projection algorithm
     */
    public void setCurrentProjectionMethod(final ProjectionMethod newProj) {
        currentProjectionMethod = newProj;
        currentProjectionMethod.project();
    }

    /**
     * @return the current projection algorithm
     */
    public ProjectionMethod getCurrentProjectionMethod() {
        return currentProjectionMethod;
    }

    /**
     * Convenience method to get upstairs dataset.
     *
     * @return hi-dimensional dataset associated with current projector
     */
    public Dataset getUpstairs() {
        if (currentProjectionMethod == null) {
            return null;
        }

        return currentProjectionMethod.getUpstairs();
    }

    /**
     * Convenience method to get downstairs dataset.
     *
     * @return low-dimensional dataset associated with current projector
     */
    public Dataset getDownstairs() {
        if (currentProjectionMethod == null) {
            return null;
        }

        return currentProjectionMethod.getDownstairs();
    }

    /**
     * Returns error, which is only set by some projection functions.
     *
     * @return current error
     */
    public double getError() {
        return error;
    }

    /**
     * @return Returns the defaultProjectionMethod.
     */
    public String getDefaultProjector() {
        return defaultProjectionMethod;
    }

    /**
     * @param defaultProjectionMethod The defaultProjectionMethod to set.
     */
    public void setDefaultProjectionMethod(final String defaultMethod) {
        this.defaultProjectionMethod = defaultMethod;
    }
    
    /**
     * @return the currentState
     */
    public double[] getCurrentState() {
        return currentState;
    }

    /**
     * @param currentState the currentState to set
     */
    public void setCurrentState(double[] currentState) {
        this.currentState = currentState;
    }
    

    @Override
    public String toString() {
        return "High Dimensional Data \n" +
            getCurrentProjectionMethod().getUpstairs().toString() +
            "Projected Data \n" +
            getCurrentProjectionMethod().getDownstairs().toString();
    }
    
    /**
     * Reset the gauge.  Clear the underlying datasets.
     */
    public void reset() {
        this.getUpstairs().clear();
        this.getDownstairs().clear();
    }
    
    /**
     * Returns the size of the dataset.
     *
     * @return size of dataset.
     */
    public int getNumPoints() {
        return getCurrentProjectionMethod().getDownstairs().getNumPoints();
    }
    
    /**
     * Returns the current projected point.
     *
     * @param index index of point to return.
     * @return the point.
     */
    public double[] getProjectedPoint(final int index) {
        if (index < getCurrentProjectionMethod().getDownstairs().getNumPoints() && index > 0) {
            return getCurrentProjectionMethod().getDownstairs().getPoint(index);
        } else {
            // throw index out of range exception
            return null;
        }
    }

    /**
     * @return the projectorSettings
     */
    public Settings getSettings() {
        return projectorSettings;
    }
}
