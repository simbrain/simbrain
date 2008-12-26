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

import java.util.ArrayList;
import java.util.Collection;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Model for a JFreeChart Scatter plot.
 */
public class ScatterPlotModel {

    /** Consumer list. */
    private ArrayList<ScatterPlotConsumer> consumers = new ArrayList<ScatterPlotConsumer>();
    
    /** Scatter Plot Data. */
    private XYSeriesCollection dataset = new XYSeriesCollection();
    
    /** Default number of sources. */
    private static final int DEFAULT_DATA_SOURCES = 5;

    /** Parent Component. */
    private ScatterPlotComponent parentComponent;


    /**
     * Scatter plot model constructor.
     * @param parent component
     */
    public ScatterPlotModel(final ScatterPlotComponent parent) {
        parentComponent = parent;
        defaultInit();
    }

    /**
     * @return JFreeChart plot dataset
     */
    public XYSeriesCollection getDataset() {
        return dataset;
    }

    /**
     * Default Initialization.
     */
    private void defaultInit() {
        addDataSources(DEFAULT_DATA_SOURCES);
    }

    /**
     * @return parent component.
     */
    public ScatterPlotComponent getParent() {
        return parentComponent;
    }

    /**
     * Set the parent component.
     *
     * @param parent the parent component
     */
    public void setParent(final ScatterPlotComponent parent) {
        parentComponent = parent;
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
        int seriesCount = dataset.getSeriesCount();
        for (int i = 0; seriesCount > i; ++i) {
            dataset.getSeries(i).clear();
        }
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(ScatterPlotModel.class, "parentComponent");
        xstream.omitField(ScatterPlotModel.class, "consumers");
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
        consumers = new ArrayList<ScatterPlotConsumer>();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<ScatterPlotConsumer> getConsumers() {
        return consumers;
    }
}
