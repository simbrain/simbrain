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
package org.simbrain.plot.piechart;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Data for a JFreeChart pie chart.
 */
public class PieChartComponent extends WorkspaceComponent {

    /** Data model. */
    private PieChartModel model;

    /** Pie chart consumer type. */
    private AttributeType pieChartConsumer;

    /**
     * Objects which can be used to set the pie chart. Component level interface
     * to plot.
     */
    private List<PieChartSetter> setterList = new ArrayList<PieChartSetter>();

    /**
     * Create new PieChart Component.
     * @param name of chart
     */
    public PieChartComponent(final String name) {
        super(name);
        model = new PieChartModel();
        model.defaultInit();
        initializeAttributes();
        addListener();
    }

    /**
     * Initializes a pie chart with a model.
     *
     * Used in deserializing.
     *
     * @param name name of component
     * @param model to use for the plot
     */
    public PieChartComponent(final String name, final PieChartModel model) {
        super(name);
        this.model = model;
        initializeAttributes();
        addListener();
    }

    /**
     * Initialize consuming attributes.
     */
    private void initializeAttributes() {
        pieChartConsumer = new AttributeType(this, "Slice", "Value",
                double.class, true);
        addConsumerType(pieChartConsumer);
        for (int i = 0; i < model.getDataset().getItemCount(); i++) {
            addSetter(i);
        }
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        if (pieChartConsumer.isVisible()) {
            for (PieChartSetter setter : setterList) {
                String description = pieChartConsumer.getSimpleDescription("Slice " + setter.getIndex());
                PotentialConsumer consumer = getAttributeManager().createPotentialConsumer(setter, pieChartConsumer, description);
               returnList.add(consumer);
            }
        }
        return returnList;
    }

    /**
     * Return the setter with specified index, or null if none found.
     *
     * @param i index of setter
     * @return the setter object
     */
    public PieChartSetter getSetter(int i) {
        for (PieChartSetter setter : setterList) {
            if (setter.getIndex() == i) {
                return setter;
            }
        }
        return null;
    }

    /**
     * Add a setter with the specified index.
     *
     * @param i index of setter
     */
    public void addSetter(int i) {
        for (PieChartSetter setter : setterList) {
            if (setter.getIndex() == i) {
                return;
            }
        }
        setterList.add(new PieChartSetter(i));
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
                if (getSetter(index) == null) {
                    addSetter(index);
                    firePotentialAttributesChanged();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void dataSourceRemoved(final int index) {
                PieChartSetter setter = getSetter(index);
                fireAttributeObjectRemoved(setter);
                setterList.remove(setter);
                firePotentialAttributesChanged();
            }

        });
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof PieChartSetter) {
            return "" + ((PieChartSetter) object).getIndex();
        }
        return null;
    }


    @Override
    public Object getObjectFromKey(String objectKey) {
        try {
            int i = Integer.parseInt(objectKey);
            PieChartSetter setter = new PieChartSetter(i);
            return  setter;
        } catch (NumberFormatException e) {
            return null; // the supplied string was not an integer
        }
    }

    /**
     * Streams file data for opening saved charts.
     * @param input stream
     * @param name file name
     * @param format format
     * @return component to be opened
     */
    public static PieChartComponent open(final InputStream input,
            final String name, final String format) {
        PieChartModel dataModel = (PieChartModel) PieChartModel.getXStream().fromXML(input);
        return new PieChartComponent(name, dataModel);
    }

    /**
     * @return the model.
     */
    public PieChartModel getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        PieChartModel.getXStream().toXML(model, output);
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
        model.updateTotalValue();
    }

    @Override
    public String getXML() {
        return PieChartModel.getXStream().toXML(model);
    }

    /**
     * Object which sets a value of one slice of a pie chart.
     */
    public class PieChartSetter {

        /** Index. */
        private Integer index;

        /**
         * Construct a setter object.
         *
         * @param index index of the "slice" to set
         */
        public PieChartSetter(final Integer index) {
            this.index = index;
        }

        /**
         * Set the value.
         *
         * @param val value for the slice
         */
        public void setValue(final double val) {
            double total = getModel().getTotal();
            if (total == 0) {
                return;
            }
            getModel().getDataset().setValue(index, val / total);
         }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }
    }

}
