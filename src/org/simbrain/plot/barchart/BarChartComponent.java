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

import javax.swing.JMenuItem;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYSeries;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Daa for a JFreeChart pie chart.
 */
public class BarChartComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Consumer list. */
    private ArrayList<BarChartConsumer> consumers= new ArrayList<BarChartConsumer>();
    
    /** JFreeChart dataset for bar charts*/
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();


    /**
     * Create new PieChart Component.
     */
    public BarChartComponent(String name) {
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
        addDataSources(6);
    }
    
    /**
     * Create specified number of set of data sources.
     * Adds these two existing data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    public void addDataSources(final int numDataSources) {
        int currentSize = consumers.size() + 1;
        for (int i = 0; i < numDataSources; i++) {
            BarChartConsumer newAttribute = new BarChartConsumer(this, "" + (currentSize + i), i);
            consumers.add(newAttribute);
        }
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // TODO omit fields
        return xstream;
    }
    
    public static BarChartComponent open(InputStream input, final String name, final String format) {
        return (BarChartComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(output);
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
        for (BarChartConsumer consumer : getConsumers()) {
            dataset.setValue(consumer.getValue(), new Integer(1), consumer.getIndex());
        }
    }


}
