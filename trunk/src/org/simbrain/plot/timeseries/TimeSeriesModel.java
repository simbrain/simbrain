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

/**
 * Data model for a time series plot.
 */
public class TimeSeriesModel extends ChartModel {

    /** Time Series Data. */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /** Default number of data sources for plot initialization. */
    private static final int INITIAL_DATA_SOURCES = 5;

    /** Should the range automatically change to reflect the data. */
    private boolean autoRange = true;

    /** Size of window. */
    private int windowSize = 100;

    /** Upper bound of the chart range. */
    private double rangeUpperBound = 1;

    /** Lower bound of the chart range. */
    private double rangeLowerBound = 0;

    /** Whether this chart if fixed width or not. */
    private boolean fixedWidth = true;

    /**
     * Time series model constructor.
     *
     * @param parent component
     */
    public TimeSeriesModel() {
        defaultInit();
    }

    /**
     * Initialize model to specified number of data sources.
     *
     * @param parent component
     */
    public TimeSeriesModel(int numDataSources) {
        addDataSources(numDataSources);
    }

    /**
     * Default plot initialization.
     */
    public void defaultInit() {
        addDataSources(INITIAL_DATA_SOURCES);
    }

    /**
     * Create specified number of set of data sources. Adds these two existing
     * data sources.
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
        XStream xstream = ChartModel.getXStream();
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        return this;
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
     * @return the upperRangeBoundary
     */
    public double getRangeUpperBound() {
        return rangeUpperBound;
    }

    /**
     * @param upperRangeBoundary the upperRangeBoundary to set
     */
    public void setRangeUpperBound(final double upperBound) {
        this.rangeUpperBound = upperBound;
        fireSettingsChanged();
    }

    /**
     * @return the lowerRangeBoundary
     */
    public double getRangeLowerBound() {
        return rangeLowerBound;
    }

    /**
     * @param lowerRangeBoundary the lowerRangeBoundary to set
     */
    public void setRangeLowerBound(final double lowerRangeBoundary) {
        this.rangeLowerBound = lowerRangeBoundary;
        fireSettingsChanged();
    }

    /**
     * Add data to this model.
     *
     * @param dataSourceIndex index of data source to use
     * @param time data for x axis
     * @param value data for y axis
     */
    public void addData(final int dataSourceIndex, final double time,
            final double value) {
        getDataset().getSeries(dataSourceIndex).add(time, value);
    }

    /**
     * Update the model; currently used to remove unused data when in
     * "fixed width" mode.
     */
    public void update() {

        // Trim appropriately if fixed width

        // TODO: This does not work well in trainer.gui. Concurrency issues. So
        // disabling for now.
        // if (isFixedWidth()) {
        // System.out.println("Dataset Size: " +
        // dataset.getSeries(0).getItemCount());
        // for (Iterator iterator = getDataset().getSeries().iterator();
        // iterator
        // .hasNext();) {
        // XYSeries series = (XYSeries) iterator.next();
        // if (series.getItemCount() > getWindowSize()) {
        // int diff = Math
        // .abs(series.getItemCount() - getWindowSize());
        // System.out.println("diff:" + diff);
        // for (int i = 0; i < diff; i++) {
        // series.remove(i);
        // }
        // }
        // }
        // }

    }

}
