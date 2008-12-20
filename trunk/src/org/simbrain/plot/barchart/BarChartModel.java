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

import java.util.ArrayList;
import java.util.Collection;

import org.jfree.data.category.DefaultCategoryDataset;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Data for a JFreeChart pie chart.
 */
public class BarChartModel {

    /** Consumer list. */
    private ArrayList<BarChartConsumer> consumers = new ArrayList<BarChartConsumer>();
    
    /** JFreeChart dataset for bar charts. */
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /** Initial number of data sources. */
    private static final int INITIAL_DATA_SOURCES = 6;

    /** Parent Component. */
    private BarChartComponent parentComponent;


    /**
     * Bar chart model constructor.
     * @param parent component
     */
    public BarChartModel(final BarChartComponent parent) {
        parentComponent = parent;
        defaultInit();
    }

    /**
     * Return JFreeChart pie dataset.
     * 
     * @return dataset
     */
    public DefaultCategoryDataset getDataset() {
        return dataset;
    }
    
    /**
     * Default initialization.
     */
    private void defaultInit() {
        addDataSources(INITIAL_DATA_SOURCES);
    }
    
    /**
     * Returns parent component.
     *
     * @return parent component.
     */
    public BarChartComponent getParent() {
        return parentComponent;
    }

    /**
     * Set the parent.
     *
     * @param parent the parent
     */
    public void setParent(final BarChartComponent parent) {
        this.parentComponent = parent;
    }

    /**
     * Create specified number of set of data sources.
     * Adds these two existing data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    public void addDataSources(final int numDataSources) {
        for (int i = 0; i < numDataSources; i++) {
            addColumn();
        }
    }

    /**
     * Adds a new column to the dataset.
     */
    public void addColumn() {
        int columnIndex = consumers.size() + 1;
        BarChartConsumer newAttribute = new BarChartConsumer(this, "BarChartData"
                + (columnIndex), columnIndex);
        consumers.add(newAttribute);
    }

    /**
     * Removes the last column from the dataset.
     */
    public void removeColumn() {
        int lastColumnIndex = dataset.getColumnCount() - 1;

        if (lastColumnIndex >= 0) {
            dataset.removeColumn(lastColumnIndex);
            consumers.remove(lastColumnIndex);
        }
    }
    
    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(BarChartModel.class, "parentComponent");
        xstream.omitField(BarChartModel.class, "consumers");
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
        consumers = new ArrayList<BarChartConsumer>();
        defaultInit();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection<BarChartConsumer> getConsumers() {
        return consumers;
    }

    /**
     * Used for debugging model.
     */
    public void debug() {
        System.out.println("------------ Debug model ------------");
        for (int i = 0; i < dataset.getRowCount(); i++) {
            for (int j = 0; j < dataset.getColumnCount(); j++) {
                System.out.println("<" + i + "," + j + "> " + dataset.getValue(i, j));
            }
        }
        System.out.println("--------------------------------------");
    }

}
