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
import java.util.Collection;
import java.util.Iterator;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Represents time series data.
 * 
 * TODO:    Ability to add and remove TimeSeriesConsumers
 *          Custom component listener to reflect number of consumers
 *          Ability to reset the plot.
 */
public class TimeSeriesPlotComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Consumer list. */
    private ArrayList<TimeSeriesConsumer> consumers= new ArrayList<TimeSeriesConsumer>();
    
    /** Time Series Data. */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /** Maximum iteration size if this chart is fixed width. */
    private int maxSize = 100;

    /** Whether this chart if fixed width or not. */
    private boolean fixedWidth = true;

    /**
     * Create new time series plot component.
     *
     * @param name name
     */
    public TimeSeriesPlotComponent(final String name) {
        super(name);
        addDataSources(10);
    }
    
    /**
     * Initializes a JFreeChart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public TimeSeriesPlotComponent(final String name, final int numDataSources) {
        super(name);
        addDataSources(numDataSources);
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
     * Clears the plot.
     */
    public void clearData() {
        int seriesCount = dataset.getSeriesCount();
        for (int i = 0; seriesCount > i; ++i) {
            dataset.getSeries(i).clear();
        }
    }

    /**
     * Removes a data source from the chart.
     */
    public void removeDataSource() {
        int lastSeriesIndex = dataset.getSeriesCount() - 1;

        if (lastSeriesIndex >= 0) {
            dataset.removeSeries(lastSeriesIndex);
            consumers.remove(lastSeriesIndex);
        }
    }

    /**
     * Adds a data source to the chart.
     */
    public void addDataSource() {
        int currentSize = consumers.size();
        TimeSeriesConsumer newAttribute = new TimeSeriesConsumer(this, ""
                + (currentSize), currentSize);
        consumers.add(newAttribute);
        dataset.addSeries(new XYSeries(currentSize));
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
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
    
    public static TimeSeriesPlotComponent open(InputStream input, final String name, final String format) {
        return (TimeSeriesPlotComponent) getXStream().fromXML(input);
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
    public Collection<TimeSeriesConsumer> getConsumers() {
        return consumers;
    }
    
    @Override
    public void update() {

        // Trim appropriately if fixed width
        if (fixedWidth) {
//            System.out.println("Dataset Size: " + dataset.getSeries(0).getItemCount());
            for (Iterator iterator = dataset.getSeries().iterator(); iterator.hasNext(); ) {
                XYSeries series = (XYSeries) iterator.next();
                if (series.getItemCount() > maxSize) {
                    series.remove(0);
                }
            }
        }


        // Add the data
        for (TimeSeriesConsumer consumer : getConsumers()) {
            dataset.getSeries(consumer.getIndex()).add(
                    getWorkspace().getTime(), consumer.getValue());
        }
    }

    /**
     * @return the dataset
     */
    public XYSeriesCollection getDataset() {
        return dataset;
    }

    /**
     * @return the maxSize
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * @param maxSize the maxSize to set
     */
    public void setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @return the fixedWidth
     */
    public boolean isFixedWidth() {
        return fixedWidth;
    }

    /**
     * @param fixedWidth the fixedWidth to set
     */
    public void setFixedWidth(final boolean fixedWidth) {
        this.fixedWidth = fixedWidth;
    }

    @Override
    public String getCurrentDirectory() {
        return "." + System.getProperty("file.separator");

    }
    
    @Override
    public String getXML() {
        return TimeSeriesPlotComponent.getXStream().toXML(this);
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
    }
}
