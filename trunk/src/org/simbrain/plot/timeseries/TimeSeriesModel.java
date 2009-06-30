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

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.plot.ChartModel;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Data model for a time series plot.
 */
public class TimeSeriesModel extends ChartModel {

    /** Time Series Data. */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /** Default number of data sources for plot initialization. */
    private static final int INITIAL_DATA_SOURCES = 5;

    /** Should fixed window size be used. */
    private boolean fixedWindow = true;

    /** Should the domain automatically change to reflect the data. */
    private boolean autoDomain = true;

    /** Should the range automatically change to reflect the data. */
    private boolean autoRange = true;

    /** Size of window. */
    private int windowSize = 100;

    /** Upper boundary of the chart domain. */
    private double upperDomainBoundary = 1;

    /** Lower boundary of the chart domain. */
    private double lowerDomainBoundary = 0;

    /** Upper boundary of the chart range. */
    private double upperRangeBoundary = 1;

    /** Lower boundary of the chart range. */
    private double lowerRangeBoundary = 0;
    
    /** Maximum iteration size if this chart is fixed width. */
    private int maxSize = 100;

    /** Whether this chart if fixed width or not. */
    private boolean fixedWidth = true;


    /**
     * Time series model constructor.
     * @param parent component
     */
    public TimeSeriesModel() {
    }

    /**
     * Default plot initialization.
     */
    public void defaultInit() {
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
        Integer lastSeriesIndex = dataset.getSeriesCount() - 1;

        if (lastSeriesIndex >= 0) {
            this.fireDataSourceRemoved(lastSeriesIndex);
            dataset.removeSeries(lastSeriesIndex);
        }
    }

    /**
     * Adds a data source to the chart.
     */
    public void addDataSource() {
        Integer currentSize = dataset.getSeriesCount();
        dataset.addSeries(new XYSeries(currentSize));
        this.fireDataSourceAdded(currentSize);
    }

    /**
     * @return JFreeChart data set.
     */
    public XYSeriesCollection getDataset() {
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
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
        return this;
    }

    /**
     * @return the fixedWindowSize
     */
    public boolean isFixedWindow() {
        return fixedWindow;
    }

    /**
     * @param fixedWindowSize the fixedWindowSize to set
     */
    public void setFixedWindow(final boolean fixedWindow) {
        this.fixedWindow = fixedWindow;
        fireSettingsChanged();
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
        fireSettingsChanged();
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
        fireSettingsChanged();
    }

    /**
     * @return the windowSize
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * @param windowSize the windowSize to set
     */
    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
        fireSettingsChanged();
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
        fireSettingsChanged();
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
        fireSettingsChanged();
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
        fireSettingsChanged();        
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
        fireSettingsChanged();        
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
        fireSettingsChanged();
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
        fireSettingsChanged();
    }
    
}
