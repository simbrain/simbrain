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

import java.io.InputStream;
import java.io.OutputStream;

import org.simbrain.plot.ChartListener;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Data for a projection component.
 * 
 * TODO:
 *  Color "hot point"
 *  Tool-tips
 *  Add ability to plot multiple projections at once.
 *  Option of connecting data-points with lines
 */
public class ProjectionComponent extends WorkspaceComponent {

    /** Data model. */
    private ProjectionModel projectionModel;

    /**
     * Default number of sources. This is the dimensionality of the hi D
     * projectionModel
     */
    private final int DEFAULT_NUMBER_OF_SOURCES = 25;

    /**
     * Create new Projection Component.
     */
    public ProjectionComponent(final String name) {
        super(name);
        projectionModel = new ProjectionModel();
        addListener();
        projectionModel.init(DEFAULT_NUMBER_OF_SOURCES);
    }

    /**
     * Initializes a JFreeChart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public ProjectionComponent(final String name, final int numDataSources) {
        super(name);
        projectionModel = new ProjectionModel();
        addListener();
        projectionModel.init(numDataSources);
    }

    /**
     * Create a projection component from an existing set of data.
     * Used in deserializing.
     *
     * @param model projection model
     * @param name name of component
     */
    public ProjectionComponent(final ProjectionModel model, final String name) {
        super(name);
        projectionModel = model;

        // Add the data to the chart.
        int numPoints = projectionModel.getProjector().getNumPoints();
        for (int i = 0; i < numPoints; i++) {
            double[] point = projectionModel.getProjector().getDownstairs().getPoint(i);
            if (point != null) {
                projectionModel.addPoint(point[0], point[1]);
            }
        }

        // Initialize attributes
        //TODO: REDO
//        this.getConsumers().clear();
//        for (int i = 0; i < projectionModel.getProjector().getDimensions(); i++) {
//            addConsumer(new ProjectionConsumer(this, i));
//        }
        addListener();
    }

    /**
     * Add chart listener to model.
     */
    private void addListener() {
        
        //TODO: REDO
//        projectionModel.addListener(new ChartListener() {
//
//            /**
//             * {@inheritDoc}
//             */
//            public void dataSourceAdded(final int index) {
//                ProjectionConsumer newAttribute = new ProjectionConsumer(
//                        ProjectionComponent.this, index);
//                addConsumer(newAttribute);
//            }
//
//            /**
//             * {@inheritDoc}
//             */
//            public void dataSourceRemoved(final int index) {
//                ProjectionConsumer toBeRemoved = (ProjectionConsumer) getConsumers()
//                        .get(index);
//                removeConsumer(toBeRemoved);
//            }
//        });

    }

    /**
     * Open component.
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
        
        //TODO: REDO

//    	// Create a new double array to be sent as a new "point" to the projection dataset
//        double[] temp = new double[getConsumers().size()];
//        int i = 0;
//        for (Consumer consumer : getConsumers()) {
//            // TODO: Class cast exception below?
//            temp[i] = ((ProjectionConsumer)consumer).getValue();
//            i++;
//        }
//        boolean newDatapointWasAdded = projectionModel.getProjector().addDatapoint(temp);
//        if (newDatapointWasAdded) {
//        	projectionModel.setCurrentItemIndex(projectionModel.getDataset().getItemCount(0));
//            resetChartDataset(); 
//        }
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
        projectionModel.getProjector().reset();
        resetChartDataset();
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
    	projectionModel.resetData();
    }

    /**
     * Used for debugging model.
     */
    public void debug() {
        System.out.println("------------ Print contents of dataset ------------");
        Projector projector = projectionModel.getProjector();
        for (int i = 0; i < projector.getNumPoints(); i++) {
                System.out.println("<" + i + "> " + projector.getProjectedPoint(i)[0] + "," + projector.getProjectedPoint(i)[1]);
            }
        System.out.println("--------------------------------------");
    }

    @Override
    public String getXML() {
        return ProjectionModel.getXStream().toXML(this);
    }

	/**
	 * @return the projectionModel
	 */
	public ProjectionModel getProjectionModel() {
		return projectionModel;
	}
}
