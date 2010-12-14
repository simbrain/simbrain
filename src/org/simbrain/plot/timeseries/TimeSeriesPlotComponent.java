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
package org.simbrain.plot.timeseries;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Represents time series data.
 *
 * TODO:    Ability to add and remove TimeSeriesConsumers
 *          Custom component listener to reflect number of consumers
 *          Ability to reset the plot.
 */
public class TimeSeriesPlotComponent extends WorkspaceComponent {

    /** The data model. */
    private final TimeSeriesModel model;

    /** Time Series consumer type. */
    private AttributeType timeSeriesConsumerType;

     /** Objects which can be used to add data to time series plot. */
    private List<TimeSeriesConsumer> consumerList = new ArrayList<TimeSeriesConsumer>();


    /**
     * Create new time series plot component.
     *
     * @param name name
     */
    public TimeSeriesPlotComponent(final String name) {
        super(name);
        model = new TimeSeriesModel();
        initializeAttributes();
        addListener();
        model.defaultInit();
    }

    /**
     * Creates a new time series component from a specified model.
     * Used in deserializing.
     *
     * @param name chart name
     * @param model chart model
     */
    public TimeSeriesPlotComponent(final String name, final TimeSeriesModel model) {
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
    public TimeSeriesPlotComponent(final String name, final int numDataSources) {
        super(name);
        model = new TimeSeriesModel();
        initializeAttributes();
        addListener();
        model.addDataSources(numDataSources);
    }



   /**
     * Initialize consuming attributes.
     */
    private void initializeAttributes() {
        timeSeriesConsumerType = new AttributeType(this, "Series", "Value", double.class, true);
        addConsumerType(timeSeriesConsumerType);
        for (int i = 0; i < model.getDataset().getSeriesCount(); i++) {
            addConsumer(i);
        }
    }

    /**
     * Return the setter with specified index, or null if none found.
     *
     * @param i index of setter
     * @return the setter object
     */
    public TimeSeriesConsumer getConsumer(int i) {
        for (TimeSeriesConsumer consumer : consumerList) {
            if (consumer.getIndex() == i) {
                return consumer;
            }
        }
        return null;
    }

    /**
     * Add a time series consumer with the specified index.
     *
     * @param i index of setter
     */
    public void addConsumer(int i) {
        for (TimeSeriesConsumer setter : consumerList) {
            if (setter.getIndex() == i) {
                return;
            }
        }
        consumerList.add(new TimeSeriesConsumer(i));
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        if (timeSeriesConsumerType.isVisible()) {
            for (TimeSeriesConsumer consumer : consumerList) {
                String description = timeSeriesConsumerType.getSimpleDescription("Time Series " + consumer.getIndex());
                PotentialConsumer consumerID = getAttributeManager()
                        .createPotentialConsumer(consumer, timeSeriesConsumerType, description);
                returnList.add(consumerID);
            }
        }
        return returnList;
    }


    /**
     * Add chart listener to model.
     */
    private void addListener() {

        model.addListener(new ChartListener() {

            /**
             * {@inheritDoc}
             */
            public void dataSourceAdded(final int index) {
                if (getConsumer(index) == null) {
                    addConsumer(index);
                    firePotentialAttributesChanged();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void dataSourceRemoved(final int index) {
                TimeSeriesConsumer consumer = getConsumer(index);
                fireAttributeObjectRemoved(consumer);
                consumerList.remove(consumer);
                firePotentialAttributesChanged();
            }
        });
    }

    /**
     * @return the model.
     */
    public TimeSeriesModel getModel() {
        return model;
    }


    @Override
    public Object getObjectFromKey(String objectKey) {
        try {
            int i = Integer.parseInt(objectKey);
            TimeSeriesConsumer setter = new TimeSeriesConsumer(i);
            return  setter;
        } catch (NumberFormatException e) {
            return null; // the supplied string was not an integer
        }
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof TimeSeriesConsumer) {
            return "" + ((TimeSeriesConsumer) object).getIndex();
        }
        return null;
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
        return this;
    }

    /**
     * Opens a saved time series plot.
     * @param input stream
     * @param name name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static TimeSeriesPlotComponent open(final InputStream input,
            final String name, final String format) {
        TimeSeriesModel dataModel = (TimeSeriesModel) TimeSeriesModel.getXStream().fromXML(input);
        return new TimeSeriesPlotComponent(name, dataModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        TimeSeriesModel.getXStream().toXML(model, output);
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
        return TimeSeriesModel.getXStream().toXML(model);
    }

    /**
     * Object which adds data to a time series plot.
     */
    public class TimeSeriesConsumer {

        /** Index. */
        private int index;

        /**
         * Construct a setter object.
         *
         * @param index index of the bar to set
         */
        public TimeSeriesConsumer(final int index) {
            this.index = index;
        }

        /**
         * Set the value.
         *
         * @param val value for the bar
         */
        public void setValue(final double val) {
            model.addData(index, TimeSeriesPlotComponent.this.getWorkspace()
                    .getTime().doubleValue(), val);
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }
    }
}
