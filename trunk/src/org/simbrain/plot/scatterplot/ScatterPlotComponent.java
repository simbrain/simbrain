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
package org.simbrain.plot.scatterplot;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JMenuItem;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import bsh.This;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Data for a JFreeChart ScatterPlot.
 */
public class ScatterPlotComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Consumer list. */
    private ArrayList<ScatterPlotConsumer> consumers= new ArrayList<ScatterPlotConsumer>();
    
    /** Scatter Plot Data. */
    private XYSeriesCollection dataset;
    
    /** Default number of sources. */
    private static final int DEFAULT_NUMBER_OF_SOURCES = 5;

    /**
     * Create new PieChart Component.
     */
    public ScatterPlotComponent(final String name) {
        super(name);
        init(DEFAULT_NUMBER_OF_SOURCES);
    }
    
    /**
     * Initializes a JFreeChart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public ScatterPlotComponent(final String name, final int numDataSources) {
        super(name);
        init(numDataSources);
    }
    
    /**
     * Initialize plot.
     *
     * @param numSources number of data sources
     */
    private void init(final int numSources) {
        this.setStrategy(Strategy.TOTAL);
        dataset = new XYSeriesCollection();
        addDataSources(numSources);
    }
    
    /**
     * Create specified number of set of data sources.
     * Adds these two existing data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    public void addDataSources(final int numDataSources) {
        for (int i = 0; i < numDataSources; i++) {
            addDataSource();
        }
    }

    /**
     * Adds a data source.
     */
    public void addDataSource() {
        int currentSize = consumers.size();
        ScatterPlotConsumer newAttribute = new ScatterPlotConsumer(this, "ScatterPlot"
                + (currentSize), currentSize);
        consumers.add(newAttribute);
        dataset.addSeries(new XYSeries(currentSize));
    }

    /**
     * Removes a data source.
     */
    public void removeDataSource() {
        int lastSeriesIndex = dataset.getSeriesCount() - 1;

        if (lastSeriesIndex >= 0) {
            dataset.removeSeries(lastSeriesIndex);
            consumers.remove(lastSeriesIndex);
        }
    }

    /**
     * Clears the chart of plotted data.
     */
    public void clearChart() {
        
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
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // TODO omit fields
        return xstream;
    }
       
    /**
     * {@inheritDoc}
     */
    public static ScatterPlotComponent open(InputStream input, final String name, final String format) {
        return (ScatterPlotComponent) getXStream().fromXML(input);
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
    public Collection<ScatterPlotConsumer> getConsumers() {
        return consumers;
    }

    @Override
    public void update() {

        // Constantly erase.   How is performance for this version?
        for (ScatterPlotConsumer consumer : getConsumers()) {
            dataset.getSeries(consumer.getIndex()).clear();
            dataset.getSeries(consumer.getIndex()).add(consumer.getX(), consumer.getY());
            //System.out.println("--[" + consumer.getIndex() + "]:" + dataset.getSeries(consumer.getIndex()).getItemCount());
        }
        
        
// THE VERSION BELOW KEEPS A HISTORY.  THERE IS NO "HOT" POINT
//        for (ScatterPlotConsumer consumer : getConsumers()) {
//            dataset.getSeries(consumer.getIndex()).add(consumer.getX(), consumer.getY());
//        }
    }
}
