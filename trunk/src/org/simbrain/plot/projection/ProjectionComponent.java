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
import java.util.ArrayList;
import java.util.List;

import org.simbrain.plot.ChartListener;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Component for a projection plot.
 *
 * TODO:
 *  Color "hot point"
 *  Tool-tips
 *  Option of connecting data-points with lines
 */
public class ProjectionComponent extends WorkspaceComponent {

    /** Data model. */
    private ProjectionModel projectionModel;

    /**
     * Default number of sources. This is the dimensionality of the hi D
     * projectionModel
     */
    private final int DEFAULT_NUMBER_OF_DIMENSIONS = 25;

    /** Time Series consumer type. */
    private AttributeType projectionConsumerType;

    /** Objects which can be used to add data to time series plot. */
    private List<Dimension> dimensionList = new ArrayList<Dimension>();

    /**
     * Create new Projection Component.
     */
    public ProjectionComponent(final String name) {
        super(name);
        projectionModel = new ProjectionModel();
        projectionModel.init(DEFAULT_NUMBER_OF_DIMENSIONS);
        initializeConsumers();
        addListener();
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
        projectionModel.init(numDataSources);
        initializeConsumers();
        addListener();
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
            double[] point = projectionModel.getProjector().getDownstairs()
                    .getPoint(i);
            if (point != null) {
                projectionModel.addPoint(point[0], point[1]);
            }
        }

        initializeConsumers();
        addListener();
    }

    /**
     * Initialize consumers.
     */
    protected void initializeConsumers() {
        dimensionList.clear();
        projectionConsumerType = new AttributeType(this, "Dimension", "setValue",
                double.class, true);
        addConsumerType(projectionConsumerType);
        for (int i = 0; i < projectionModel.getProjector().getDimensions(); i++) {
            addDimension(i);
        }
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        if (projectionConsumerType.isVisible()) {
            for (Dimension consumer : dimensionList) {
                String description = projectionConsumerType
                        .getSimpleDescription("Dimension "
                                + (consumer.getDimension() + 1));
                PotentialConsumer consumerID = getAttributeManager()
                        .createPotentialConsumer(consumer,
                                projectionConsumerType, description);
                returnList.add(consumerID);
            }
        }
        return returnList;
    }

    /**
     * Add a dimension object.
     *
     * @param i index of dimension object
     */
    protected void addDimension(int i) {
        for (Dimension dimension : dimensionList) {
            if (dimension.getDimension() == i) {
                return;
            }
        }
        dimensionList.add(new Dimension(i));
    }

    /**
     * Set number of dimensions to specified amount.
     *
     * @param numDims number of dimensions.
     */
    protected void setDimensions(int numDims) {
        for (Dimension dimension : dimensionList) {
            fireAttributeObjectRemoved(dimension);
        }
        dimensionList.clear();
        for (int i = 0; i < numDims; i++) {
            addDimension(i);
        }
    }

    /**
     * Return a dimension object.
     *
     * @param i index of dimension object to return
     * @return the dimension object
     */
    public Dimension getDimension(int i) {
        for (Dimension dimension : dimensionList) {
            if (dimension.getDimension() == i) {
                return dimension;
            }
        }
        return null;
    }

    /**
     * Add chart listener to model.
     */
    private void addListener() {

        projectionModel.addListener(new ChartListener() {

            /**
             * {@inheritDoc}
             */
            public void dataSourceAdded(final int dimension) {
                if (getDimension(dimension) == null) {
                    addDimension(dimension);
                    ProjectionComponent.this.firePotentialAttributesChanged();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void dataSourceRemoved(final int index) {
                Dimension dimension = getDimension(index);
                if (dimension != null) {
                    fireAttributeObjectRemoved(dimension);
                    dimensionList.remove(dimension);
                    ProjectionComponent.this.firePotentialAttributesChanged();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void chartInitialized(int numSources) {
                setDimensions(numSources);
                ProjectionComponent.this.firePotentialAttributesChanged();
            }

        });

    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        try {
            int i = Integer.parseInt(objectKey);
            Dimension dimension = getDimension(i);
            return  dimension;
        } catch (NumberFormatException e) {
            return null; // the supplied string was not an integer
        }
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof Dimension) {
            return "" + ((Dimension) object).getDimension();
        }
        return null;
    }

    @Override
    public String getXML() {
        return ProjectionModel.getXStream().toXML(this);
    }

    /**
     * Opens a saved projection component.
     *
     * @param input stream
     * @param name name of file
     * @param format format
     * @return component to be opened
     */
    public static ProjectionComponent open(InputStream input,
            final String name, final String format) {
        ProjectionModel model = (ProjectionModel)ProjectionModel.getXStream().fromXML(input);
        return new ProjectionComponent(model, name);
    }

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
     * Get the current state of the dimension objects, send this to the
     * projection algorithm, and update the graphics.
     */
    @Override
    public void update() {
        // System.out.println(setterList.size() + "  " +
        // projectionModel.getProjector().getDimensions());

        // Create a new double array to be sent as a new "point" to the
        // projection dataset
        double[] point = new double[dimensionList.size()];
        int i = 0;
        for (Dimension dimension : dimensionList) {
            point[i] = dimension.getValue();
            i++;
        }
        // System.out.println(Arrays.toString(temp));
        boolean newDatapointWasAdded = projectionModel.getProjector()
                .addDatapoint(point);
        if (newDatapointWasAdded) {
            resetChartDataset();
        }

        // Notify Gui that this component was updated.
        fireUpdateEvent();
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
     * Update the entire dataset. Called when the entire chart dataset is
     * changed.
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

	/**
	 * @return the projectionModel
	 */
	public ProjectionModel getProjectionModel() {
		return projectionModel;
	}

    /**
     * Object which adds data to one dimension of a projection component.
     */
    public class Dimension {

        /** Index. */
        private int dimension;

        /** Value of this dimension proxy object. */
        private double value = 0;

        /**
         * Construct a Dimension object.
         *
         * @param index index of the bar to set
         */
        public Dimension(final int index) {
            this.dimension = index;
        }

        public double getValue() {
            return value;
        }

        /**
         * Set the value.
         *
         * @param val value for the bar
         */
        public void setValue(final double val) {
            value = val;
        }

        /**
         * @return the index
         */
        public int getDimension() {
            return dimension;
        }

    }
}
