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
import java.util.Collection;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Daa for a JFreeChart pie chart.
 */
public class BarChartComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Consumer list. */
    private ArrayList<BarChartConsumer> consumers = new ArrayList<BarChartConsumer>();
    
    /** JFreeChart dataset for bar charts. */
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /** Initial number of data sources. */
    private static final int INITIAL_DATA_SOURCES = 6;


    /**
     * Create new BarChart Component.
     *
     * @param name chart name
     */
    public BarChartComponent(final String name) {
        super(name);
        defaultInit();
    }
    
    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public BarChartComponent(final String name, final int numDataSources) {
        super(name);
        addDataSources(numDataSources);
    }
   
    /**
     * Return JFreeChart pie dataset.
     * 
     * @return dataset
     */
    public CategoryDataset getDataset() {
        return dataset;
    }
    
    /**
     * Default initialization.
     */
    private void defaultInit() {
        addDataSources(INITIAL_DATA_SOURCES);
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
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
//        xstream.omitField(BarChartComponent.class, "logger");
        xstream.omitField(WorkspaceComponent.class, "component");
        xstream.omitField(WorkspaceComponent.class, "listenerList");
        xstream.omitField(WorkspaceComponent.class, "workspace");
        xstream.omitField(WorkspaceComponent.class, "logger");
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
        System.out.println("ReadResolve.");
        return this;
    }
    
    public static BarChartComponent open(InputStream input, final String name, final String format) {
        return (BarChartComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(this, output);
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
     * {@inheritDoc}
     */
    public Collection<BarChartConsumer> getConsumers() {
        return consumers;
    }

    @Override
    public void update() {
        System.out.println(dataset);
        for (BarChartConsumer consumer : getConsumers()) {
            dataset.setValue(consumer.getValue(), new Integer(1), consumer.getIndex());
        }
    }

    @Override
    public String getCurrentDirectory() {
        return "." + System.getProperty("file.separator");

    }
    
    @Override
    public String getXML() {
        return BarChartComponent.getXStream().toXML(this);
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
    }
}
