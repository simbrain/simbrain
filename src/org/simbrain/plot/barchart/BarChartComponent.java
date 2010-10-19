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
package org.simbrain.plot.barchart;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Data for a JFreeChart bar chart.
 */
public class BarChartComponent extends WorkspaceComponent {

    /** Data model. */
    private BarChartModel model;

    /**
     * Objects which can be used to set bar chart. Component level interface to
     * plot.
     */
    private List<BarChartSetter> setterList = new ArrayList<BarChartSetter>();

    /**
     * Create new BarChart Component.
     *
     * @param name chart name
     */
    public BarChartComponent(final String name) {
        super(name);
        model = new BarChartModel();
        addListener();
        model.defaultInit();
        initializeAttributes();
    }

    /**
     * Create new BarChart Component from a specified model.
     * Used in deserializing.
     *
     * @param name chart name
     * @param model chart model
     */
    public BarChartComponent(final String name, final BarChartModel model) {
        super(name);
        this.model = model;
        initializeAttributes();
        addListener();
    }

    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public BarChartComponent(final String name, final int numDataSources) {
        super(name);
        model = new BarChartModel();
        addListener();
        model.addDataSources(numDataSources);
        initializeAttributes();
    }

    /**
     * Initialize consuming attributes.
     */
    private void initializeAttributes() {

        addConsumerType(new AttributeType(this, "Dimension", null,
                double.class, true));

        //TODO: Move this
        for (int i = 0; i < model.getDataset().getColumnCount(); i++) {
            setterList.add(new BarChartSetter(i));
        }
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
                firePotentialAttributesChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void dataSourceRemoved(final int index) {
                if (getSetter(index) != null) {
                    fireAttributeObjectRemoved(getSetter(index));
                }
                firePotentialAttributesChanged();
            }
        });
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        try {
            int i = Integer.parseInt(objectKey);
            BarChartSetter setter = new BarChartSetter(i);
            setterList.add(setter);
            return  setter;
        } catch (NumberFormatException e) {
            return null; // the supplied string was not an integer
        }
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof BarChartSetter) {
            return "" + ((BarChartSetter) object).getIndex();
        }
        return null;
    }

    /**
     * Returns model.
     *
     * @return the model.
     */
    public BarChartModel getModel() {
        return model;
    }

    /**
     * Opens a saved bar chart.
     *
     * @param input stream
     * @param name name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static BarChartComponent open(final InputStream input,
            final String name, final String format) {
        BarChartModel dataModel = (BarChartModel) BarChartModel.getXStream()
                .fromXML(input);
        return new BarChartComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        BarChartModel.getXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public void closing() {
    }

    @Override
    public String getXML() {
        return BarChartModel.getXStream().toXML(model);
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        for (int i = 0; i < model.getDataset().getColumnCount(); i++) {
            BarChartSetter setter =  new BarChartSetter(i);
            PotentialConsumer consumerID = new PotentialConsumer(
                    this,
                    setter,
                    "Bar_" + i,
                    "Value", double.class);
            setterList.add(setter);
            returnList.add(consumerID);
        }
        return returnList;
    }


    /**
     * Return the setter with specified index, or null if none found.
     *
     * @param i index of setter
     * @return the setter object
     */
    public BarChartSetter getSetter(int i) {
        for(BarChartSetter setter : setterList) {
            if(setter.getIndex() == i) {
                return setter;
            }
        }
        return null;
    }

    /**
     * Object which sets a value of one bar in a bar chart.
     */
    public class BarChartSetter {

        /** Index. */
        private int index;

        /**
         * Construct a setter object.
         *
         * @param index index of the bar to set
         */
        public BarChartSetter(final int index) {
            this.index = index;
        }

        /**
         * Set the value.
         *
         * @param val value for the bar
         */
        public void setValue(final double val) {
            model.setValue(val, index);
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }
    }

}
