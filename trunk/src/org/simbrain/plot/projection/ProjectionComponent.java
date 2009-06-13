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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * Data for a projection component.
 * 
 * TODO:
 *  Color "hot point"
 *  Tool-tips
 *  Add ability to plot multiple projections at once.
 *  Option of connecting data-points with lines
 */
public class ProjectionComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Data model. */
    private ProjectionModel projectionModel;

    /** Scatter Plot Data. */
    private XYSeriesCollection dataset;
    
    /**
     * Default number of sources. This is the dimensionality of the hi D
     * projectionModel
     */
    private final int DEFAULT_NUMBER_OF_SOURCES = 25;
    
    /** Flag which allows the user to start and stop iterative projection techniques.. */
    private volatile boolean isRunning = true;

    /** Flag for checking that GUI update is completed. */
    private volatile boolean setUpdateCompleted;
    
    /**
     * Create new Projection Component.
     */
    public ProjectionComponent(final String name) {
        super(name);
        projectionModel = new ProjectionModel(DEFAULT_NUMBER_OF_SOURCES);
        init(DEFAULT_NUMBER_OF_SOURCES);
    }
    
    /**
     * Initializes a JFreeChart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public ProjectionComponent(final String name, final int numDataSources) {
        super(name);
        projectionModel = new ProjectionModel(numDataSources);
        init(numDataSources);
    }
    
    /**
     * Create a projection component from an existing set of data.
     *
     * @param model projection model
     * @param name name of component
     */
    public ProjectionComponent(final ProjectionModel model, final String name) {
        super(name);
        projectionModel = model;
        int numPoints = projectionModel.getProjector().getNumPoints();
        this.setAttributeListingStyle(AttributeListingStyle.TOTAL);
        dataset = new XYSeriesCollection();
        dataset.addSeries(new XYSeries("Data", false, true));
        
        // Initialize consuming attributes
        getConsumers().clear();
        for (int i = 0; i < projectionModel.getProjector().getDimensions(); i++) {
            int currentSize = getConsumers().size() + 1;
            ProjectionConsumer newAttribute = new ProjectionConsumer(this,
                    "Dimension" + currentSize, currentSize);
            getConsumers().add(newAttribute);
        }
        
        // Add the data to the chart.
        for (int i = 0; i < numPoints; i++) {
            double[] point = projectionModel.getProjector().getProjectedPoint(i);
            if (point != null) {
                dataset.getSeries(0).add(point[0], point[1], true);
            }
        }

    }
    
    /**
     * Initialize plot.
     *
     * @param numSources number of data sources
     */
    private void init(final int numSources) {
        this.setAttributeListingStyle(AttributeListingStyle.TOTAL);
        dataset = new XYSeriesCollection();
        dataset.addSeries(new XYSeries("Data", false, true));
        for (int i = 0; i < numSources; i++) {
            addSource();
        }
    }

    /**
     * Adds a consuming attribute. Increases the dimensionality of the projected
     * data by one.
     */
    public void addSource() {
        int currentSize = getConsumers().size() + 1;
        ProjectionConsumer newAttribute = new ProjectionConsumer(this,
                "Dimension" + currentSize, currentSize);
        addConsumer(newAttribute);
        projectionModel.getProjector().init(currentSize);
    }

    /**
     * Removes a source from the dataset.
     */
    public void removeSource() {
        int currentSize = getConsumers().size() - 1;

        if (currentSize > 0) {
        	//dataset.removeSeries(lastSeriesIndex);
            getConsumers().remove(currentSize);
            projectionModel.getProjector().init(currentSize);
        }
    }
 
    /**
     * Return JFreeChart xy dataset.
     * 
     * @return dataset
     */
    public XYDataset getDataset() { 
        return dataset;
    }

    /**
     * {@inheritDoc}
     */
    public static ProjectionComponent open(InputStream input,
            final String name, final String format) {
        return new ProjectionComponent((ProjectionModel) ProjectionModel.getXStream().fromXML(input), name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        projectionModel.getProjector().getUpstairs().preSaveInit();
        projectionModel.getProjector().getDownstairs().preSaveInit();
        ProjectionModel.getXStream().toXML(projectionModel, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

   
    /**
     * Get the current state of the consumers, send this to the projection algorithm,
     * and update the graphics.
     */
    @Override
    public void update() {

    	// Create a new double array to be sent as a new "point" to the projection dataset
        double[] temp = new double[getConsumers().size()];
        int i = 0;
        for (Consumer consumer : getConsumers()) {
            // TODO: Class cast exception below?
            temp[i] = ((ProjectionConsumer)consumer).getValue();
            i++;
        }
        boolean newDatapointWasAdded = projectionModel.getProjector().addDatapoint(temp);
        if (newDatapointWasAdded) {
            resetChartDataset(); // (should rename; see below)
            // TODO: Add a check to see whether the current projection algorith
            //		 resets all the data or simply involves adding a single  new datapoint
            //		 If only one new datapoint is added it should be added to the dataset
            //		  and "resetChartDataset" should not be called.
        }
    }
    
    /**
     * Get reference to underlying projector object.
     *
     * @return projector object.
     */
    public Projector getGauge() {
        return projectionModel.getProjector();
    }
    
    /**
     * Clear the dataset.
     */
    public void clearData() {
        dataset.getSeries(0).clear();
        projectionModel.getProjector().reset();
        fireUpdateEvent();
    }
    
    /**
     * Change projection.
     */
    public void changeProjection() {
        projectionModel.getProjector().getCurrentProjectionMethod().project(); // Should this have happened already?
        resetChartDataset();
    }
    
    /**
     * Update the entire dataset.  Called when the entire chart dataset is changed.
     */
    public void resetChartDataset() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Add the data
                dataset.getSeries(0).clear();
                int size = projectionModel.getProjector().getNumPoints();
                for (int i = 0; i < size - 2; i++) {
                    double[] point = projectionModel.getProjector().getProjectedPoint(i);
                    if(point != null) {
                    	// No need to update the chart yet (hence the "false" parameter)
                    	dataset.getSeries(0).add(point[0], point[1], false);
                    }
                }
                // Notify chart when last datapoint is updated
                double[] point = projectionModel.getProjector().getProjectedPoint(size-1);
                if (point != null) {
                    dataset.getSeries(0).add(point[0], point[1], true);
                }
                setUpdateCompleted(true);
            }
        });
    }

    /**
     * Used for debugging model.
     */
    public void debug() {
        System.out.println("------------ Print contents of dataset ------------");
        for (int i = 0; i < dataset.getSeries(0).getItemCount(); i++) {
                System.out.println("<" + i + "> " + dataset.getSeries(0).getDataItem(i).getX() + "," + dataset.getSeries(0).getDataItem(i).getY());
            }
        System.out.println("--------------------------------------");
    }

    @Override
    public String getXML() {
        return ProjectionModel.getXStream().toXML(this);
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
    public void setUpdateCompleted(boolean b) {
        setUpdateCompleted = b;
    }

    /**
     * Swing update flag.
     * 
     * @return whether update is completd or not
     */
    public boolean isUpdateCompleted() {
        return setUpdateCompleted;
    }
}
