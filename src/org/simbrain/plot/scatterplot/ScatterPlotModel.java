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

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.plot.ChartModel;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Model for a JFreeChart Scatter plot.
 */
public class ScatterPlotModel extends ChartModel {

    /** Consumer list. */
    private ArrayList<ScatterPlotConsumer> consumers = new ArrayList<ScatterPlotConsumer>();
    
    /** Scatter Plot Data. */
    private XYSeriesCollection dataset = new XYSeriesCollection();
    
    /** Default number of sources. */
    private static final int DEFAULT_DATA_SOURCES = 5;

    /** Parent Component. */
    private ScatterPlotComponent parentComponent;

    /** Auto domain. */
    private boolean autoDomain = false;

    /** Auto range. */
    private boolean autoRange = false;

    /** Upper range boundary. */
    private double upperRangeBoundary = 10;

    /** Lower range boundary. */
    private double lowerRangeBoundary = 0;

    /** Upper domain boundary. */
    private double upperDomainBoundary = 10;

    /** Lower domain boundary. */
    private double lowerDomainBoundary = 0;

    /** Size of chart dot. */
    private int dotSize = 1;

    /** Color for series. */
    private List<Paint> chartSeriesPaint = new LinkedList<Paint>();


    /**
     * Scatter plot model constructor.
     * @param parent component
     */
    public ScatterPlotModel(final ScatterPlotComponent parent) {
        parentComponent = parent;
        setChartColors();
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
     * Sets the series colors for the chart.
     */
    private void setChartColors() {
        chartSeriesPaint.add(Color.RED);
        chartSeriesPaint.add(Color.BLUE);
        chartSeriesPaint.add(Color.GREEN);
        chartSeriesPaint.add(Color.YELLOW);
        chartSeriesPaint.add(Color.PINK);
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
            chartSeriesPaint.remove(lastSeriesIndex);
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
     * Updates the chart.
     */
    public void update() {
        getParent().updateSettings();
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

    /**
     * @return the autoDomain
     */
    public boolean isAutoDomain() {
        return autoDomain;
    }

    /**
     * @param autoDomain the autoDomain to set
     */
    public void setAutoDomain(final boolean autoDomain) {
        this.autoDomain = autoDomain;
    }

    /**
     * @return the autoRange
     */
    public boolean isAutoRange() {
        return autoRange;
    }

    /**
     * @param autoRange the autoRange to set
     */
    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    /**
     * @return the upperRangeBoundary
     */
    public double getUpperRangeBoundary() {
        return upperRangeBoundary;
    }

    /**
     * @param upperRangeBoundary the upperRangeBoundary to set
     */
    public void setUpperRangeBoundary(final double upperRangeBoundary) {
        this.upperRangeBoundary = upperRangeBoundary;
    }

    /**
     * @return the lowerRangeBoundary
     */
    public double getLowerRangeBoundary() {
        return lowerRangeBoundary;
    }

    /**
     * @param lowerRangeBoundary the lowerRangeBoundary to set
     */
    public void setLowerRangeBoundary(final double lowerRangeBoundary) {
        this.lowerRangeBoundary = lowerRangeBoundary;
    }

    /**
     * @return the upperDomainBoundary
     */
    public double getUpperDomainBoundary() {
        return upperDomainBoundary;
    }

    /**
     * @param upperDomainBoundary the upperDomainBoundary to set
     */
    public void setUpperDomainBoundary(final double upperDomainBoundary) {
        this.upperDomainBoundary = upperDomainBoundary;
    }

    /**
     * @return the lowerDomainBoundary
     */
    public double getLowerDomainBoundary() {
        return lowerDomainBoundary;
    }

    /**
     * @param lowerDomainBoundary the lowerDomainBoundary to set
     */
    public void setLowerDomainBoundary(final double lowerDomainBoundary) {
        this.lowerDomainBoundary = lowerDomainBoundary;
    }

    /**
     * @return the dotSize
     */
    public int getDotSize() {
        return dotSize;
    }

    /**
     * @param dotSize the dotSize to set
     */
    public void setDotSize(final int dotSize) {
        this.dotSize = dotSize;
    }

    /**
     * @return the paint color
     */
    public List<Paint> getChartSeriesPaint() {
        return chartSeriesPaint;
    }

    /**
     * @param chartSeriesPaint the paint color to set
     */
    public void setChartSeriesPaint(final List<Paint> chartSeriesPaint) {
        this.chartSeriesPaint = chartSeriesPaint;
    }
}
