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
package org.simbrain.plot.projection;

import java.awt.EventQueue;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.plot.ChartModel;
import org.simbrain.util.projection.Dataset;
import org.simbrain.util.projection.ProjectionMethod;
import org.simbrain.util.projection.Projector;

import com.thoughtworks.xstream.XStream;

/**
 * Main data for a projection chart.
 */
public class ProjectionModel extends ChartModel {

    /** High Dimensional Projection. */
    private Projector projector = new Projector();

    /** Scatter Plot Data. */
    private XYSeriesCollection dataset;

    /** Flag which allows the user to start and stop iterative projection techniques.. */
    private volatile boolean isRunning = true;

    /** Flag for checking that GUI update is completed. */
    private volatile boolean setUpdateCompleted;

    /** Index of current item; used to paint it a different color. */
	private int currentItemIndex = 0;

    /**
     * Default constructor.
     *
     * @param numDataSources dimension of the data.
     */
    public ProjectionModel() {
    }

    /**
     * Initialize the projection model with a certain number of data sources.
     * 
     * @param numDataSources
     *            number of sources to initialize model with.
     */
    public void init(final int numDataSources) {
        dataset = new XYSeriesCollection();
        dataset.addSeries(new XYSeries("Data", false, true));
        for (int i = 0; i < numDataSources; i++) {
            addSource();
        }
    }

    /**
     * Adds a consuming attribute. Increases the dimensionality of the projected
     * data by one.
     */
    public void addSource() {
    	int index = projector.getDimensions() + 1;
    	projector.init(index);
        fireDataSourceAdded(index);
    }

    /**
     * Removes a source from the dataset.
     */
    public void removeSource() {
        int currentSize = projector.getDimensions()  - 1;

        if (currentSize > 0) {
            projector.init(currentSize);
            fireDataSourceRemoved(currentSize);
        }
    }

    /**
     * Returns the projector.
     *
     * @return the projector.
     */
    public Projector getProjector() {
        return projector;
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = ChartModel.getXStream();
        xstream.omitField(Projector.class, "logger");
        xstream.omitField(Projector.class, "currentState");
        xstream.omitField(ProjectionMethod.class, "logger");
        xstream.omitField(Dataset.class, "logger");
        xstream.omitField(Dataset.class, "distances");
        xstream.omitField(Dataset.class, "dataset");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     * 
     * @return Initialized object.
     */
    private Object readResolve() {
        projector.getUpstairs().postOpenInit();
        projector.getDownstairs().postOpenInit();
        return this;
    }

	/**
	 * @return the dataset
	 */
	public XYSeriesCollection getDataset() {
		return dataset;
	}

	/** 
	 * Convenience method for adding points to dataset.
	 *
	 * @param x x dimension of point to add
	 * @param y y dimension of point to add
	 */
	public void addPoint(double x, double y) {
		dataset.getSeries(0).add(x, y, true);
    }
	
	/**
	 * Resets the JFreeChart data and re-adds all the datapoint. Invoked when the projector must be
	 * applied to an entire dataset.
	 */
	public void resetData() {
		// TODO: Add a check to see whether the current projection algorithm
		// resets all the data or simply involves adding a single new datapoint.
		// Add a property to projectionMethods like
		// newPointsAffectWholeDataset(). If true, then this should be called;
		// otherwise it should not have to be.
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				// Add the data
				dataset.getSeries(0).clear();
				int size = projector.getNumPoints();
				for (int i = 0; i < size - 2; i++) {
					double[] point = projector.getProjectedPoint(i);
					if (point != null) {
						// No need to update the chart yet (hence the "false"
						// parameter)
						dataset.getSeries(0).add(point[0], point[1], false);
					}
				}				
				// Notify chart when last datapoint is updated
				double[] point = projector.getProjectedPoint(size - 1);
				if (point != null) {
					dataset.getSeries(0).add(point[0], point[1], true);
				}
				setUpdateCompleted(true);
			}
		});

	}

    /**
     * @return whether this component being updated by a thread or not.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * This flag allows the user to start and stop iterative projection
     * techniques.
     * 
     * @param b whether this component being updated by a thread or not.
     */
    public void setRunning(boolean b) {
        isRunning = b;
    }

    /**
     * Swing update flag.
     * 
     * @param b whether updated is completed
     */
    public void setUpdateCompleted(final boolean b) {
        setUpdateCompleted = b;
    }

    /**
     * Swing update flag.
     * 
     * @return whether update is completed or not
     */
    public boolean isUpdateCompleted() {
        return setUpdateCompleted;
    }

	/**
	 * @return the currentItemIndex
	 */
	public int getCurrentItemIndex() {
		return currentItemIndex;
	}

	/**
	 * @param currentItemIndex the currentItemIndex to set
	 */
	public void setCurrentItemIndex(int currentItemIndex) {
		this.currentItemIndex = currentItemIndex;
	}

}
