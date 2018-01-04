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
package org.simbrain.plot.rasterchart;

import java.io.InputStream;
import java.io.OutputStream;

import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.Consumible;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Represents raster data.
 */
public class RasterPlotComponent extends WorkspaceComponent {

    /** The data model. */
    private final RasterModel model;

    /** Raster Plot consumer type. */
//    private AttributeType rasterPlotConsumerType;

    /**
     * Create new raster plot component.
     *
     * @param name name
     */
    public RasterPlotComponent(final String name) {
        super(name);
        model = new RasterModel();
        initializeAttributes();
        addListener();
        model.defaultInit();
    }

    /**
     * Creates a new raster plot component from a specified model. Used in
     * deserializing.
     *
     * @param name chart name
     * @param model chart model
     */
    public RasterPlotComponent(final String name,
            final RasterModel model) {
        super(name);
        this.model = model;
        initializeAttributes();
        addListener();
    }

    /**
     * Initializes a JFreeChart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public RasterPlotComponent(final String name, final int numDataSources) {
        super(name);
        model = new RasterModel(numDataSources);
        initializeAttributes();
        addListener();
    }

    /**
     * Initialize consuming attributes.
     */
    private void initializeAttributes() {
//        rasterPlotConsumerType = new AttributeType(this, "Series", "setValue",
//                double.class, true);
//        addConsumerType(rasterPlotConsumerType);
    }

//    @Override
//    public List<PotentialConsumer> getPotentialConsumers() {
//        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
//        if (rasterPlotConsumerType.isVisible()) {
//            for (int i = 0; i < model.getDataset().getSeriesCount(); i++) {
//                String description = rasterPlotConsumerType
//                        .getSimpleDescription("Raster Series " + (i + 1));
//                PotentialConsumer consumer = getAttributeManager()
//                        .createPotentialConsumer(this, "setValues",
//                                new Class[] { double[].class, Integer.class },
//                                new Object[] { i });
//                consumer.setCustomDescription(description);
//                returnList.add(consumer);
//            }
//        }
//        return returnList;
//    }

    /**
     * Add chart listener to model.
     */
    private void addListener() {

        model.addListener(new ChartListener() {

            /**
             * {@inheritDoc}
             */
            public void dataSourceAdded(final int index) {
//                firePotentialAttributesChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void dataSourceRemoved(final int index) {
//                firePotentialAttributesChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void chartInitialized(int numSources) {
                // No implementation yet (not used in this component thus far).
            }

        });
    }

    /**
     * @return the model.
     */
    public RasterModel getModel() {
        return model;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        return this;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        return this;
    }

    /**
     * Opens a saved raster plot.
     *
     * @param input stream
     * @param name name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static RasterPlotComponent open(final InputStream input,
            final String name, final String format) {
        RasterModel dataModel = (RasterModel) RasterModel
                .getXStream().fromXML(input);
        return new RasterPlotComponent(name, dataModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        RasterModel.getXStream().toXML(model, output);
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

    @Override
    public void update() {
        model.update();
    }

    @Override
    public String getXML() {
        return RasterModel.getXStream().toXML(model);
    }

    /**
     * Set the value of a specified data source (one curve in the raster 
     * plot). This is the main method for updating the data in a raster plot
     *
     * @param values the current "y-axis" value for the raster series
     * @param index which raster series to set.
     */
    public void setValues(final double[] values, final Integer index) {
        // TODO: Throw exception if index out of current bounds
        for (int i = 0, n = values.length; i < n; i++) {
            model.addData(index, RasterPlotComponent.this.getWorkspace()
                .getTime(), values[i]);
        }
    }

    @Consumible
    public void setValues(final double[] values) {
        setValues(values, 0);
    }

}
