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

import com.thoughtworks.xstream.XStream;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.plot.ChartDataSource;
import org.simbrain.plot.ChartModel;
import org.simbrain.workspace.Consumable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Data model for a time series plot.
 */
public class TimeSeriesModel extends ChartModel {

    /**
     * Encapsulates a single time series.
     */
    public class TimeSeries implements ChartDataSource {

        /**
         * The time series index to write data.
         */
        private int seriesIndex;

        /**
         * Construct the time series.
         */
        TimeSeries(int seriesIndex) {
            this.seriesIndex = seriesIndex;
        }

        /**
         * Get the description.
         */
        public String getDescription() {
            return dataset.getSeries(seriesIndex).getDescription();
        }

        @Consumable(idMethod = "getDescription")
        public void setValue(double value) {
            addData(seriesIndex, timeSupplier.get(), value);
        }
    }

    /**
     * Default number of data sources for plot initialization.
     */
    private static final int INITIAL_DATA_SOURCES = 5;

    /**
     * Time Series Data.
     */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /**
     * Lambda to supply time to the time series model.
     */
    private transient Supplier<Integer> timeSupplier;

    /**
     * Should the range automatically change to reflect the data.
     */
    private boolean autoRange = true;

    /**
     * Upper bound of the chart range.
     */
    private double rangeUpperBound = 1;

    /**
     * Lower bound of the chart range.
     */
    private double rangeLowerBound = 0;

    /**
     * Set the maximum number of data points per series to plot.
     */
    private int maxDataPoints = 1000;

    /**
     * List of time series objects which can be coupled to.
     */
    private List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>();

    /**
     * Time series model constructor.
     */
    public TimeSeriesModel(Supplier<Integer> timeSupplier) {
        this.timeSupplier = timeSupplier;
        addDataSource(); // Init with at least one data source
    }

    /**
     * Create specified number of data sources.
     *
     * @param numDataSources number of data sources to add to the plot.
     */
    public void addDataSources(int numDataSources) {
        for (int i = 0; i < numDataSources; i++) {
            addDataSource();
        }
    }

    /**
     * Adds a data source to the chart with a default description.
     */
    public ChartDataSource addDataSource() {
        String description = "TimeSeries" + (timeSeriesList.size() + 1);
        return addDataSource(description);
    }

    /**
     * Adds a data source to the chart with the specified description.
     */
    public ChartDataSource addDataSource(String description) {
        int currentSize = dataset.getSeriesCount();
        XYSeries xy = new XYSeries(currentSize);
        xy.setMaximumItemCount(maxDataPoints);
        xy.setDescription(description);
        dataset.addSeries(xy);
        TimeSeries series = new TimeSeries(currentSize);
        timeSeriesList.add(series);
        this.fireDataSourceAdded(series);
        return series;
    }

    @Consumable(idMethod = "getId")
    public void addVector(double[] newPoint) {
        if (newPoint.length != timeSeriesList.size()) {
            timeSeriesList.clear();
            for (int i = 0; i < newPoint.length; i++) {
                addDataSource();
            }
        }
        for (int i = 0; i < newPoint.length; i++) {
            timeSeriesList.get(i).setValue(newPoint[i]);
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
            TimeSeries series = timeSeriesList.remove(lastSeriesIndex);
            this.fireDataSourceRemoved(series);
        }
    }

    /**
     * Remove the specified data source, if it exists.
     */
    @Override
    public void removeDataSource(ChartDataSource source) {
        if (source instanceof TimeSeries) {
            TimeSeries series = (TimeSeries) source;
            if (timeSeriesList.remove(series)) {
                dataset.removeSeries(series.seriesIndex);
                fireDataSourceRemoved(series);
            }
        }
    }

    @Override
    public Optional<? extends ChartDataSource> getDataSource(String description) {
        return getTimeSeries(description);
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
     * Returns the maximum number of data points (corresponds to time steps) to plot for each time series.
     */
    public int getMaximumDataPoints() {
        return maxDataPoints;
    }

    /**
     * Set the maximum number of data points to (corresponds to time steps) to plot for each time series.
     */
    public void setMaximumDataPoints(int value) {
        maxDataPoints = value;
        for (Object s : dataset.getSeries()) {
            ((XYSeries) s).setMaximumItemCount(value);
        }
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
     * @param upperBound the upperRangeBoundary to set
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
     * Find matching time series object. Used to deserialize.
     *
     * @param description key
     * @return matching time series
     */
    public Optional<TimeSeries> getTimeSeries(String description) {
        for (TimeSeries series : timeSeriesList) {
            if (series.getDescription().equals(description)) {
                return Optional.of(series);
            }
        }
        return Optional.empty();
    }

    /**
     * Add data to this model.
     *
     * @param dataSourceIndex index of data source to use
     * @param time            data for x axis
     * @param value           data for y axis
     */
    public void addData(int dataSourceIndex, double time, double value) {
        getDataset().getSeries(dataSourceIndex).add(time, value);
    }

    /**
     * Add data using first time series and timesupplier provided at construction.
     *
     * @param value value to add.
     */
    public void addData(double value) {
        addData(0, timeSupplier.get(), value);
    }

    /**
     * Update the model
     */
    public void update() {
    }

    /**
     * Returns a list of time series.
     */
    public List<TimeSeries> getTimeSeriesList() {
        return timeSeriesList;
    }

    /**
     * @param timeSupplier the timeSupplier to set
     */
    public void setTimeSupplier(Supplier<Integer> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    /**
     * This method is used only for coupling to the chart. It does not do anything.
     * Couplings to this method will be replaced by couplings to new data sources by the
     * ChartCouplingListener.
     */
    @Consumable(idMethod = "getName")
    public void addTimeSeries(double value) {
    }

    /**
     * Return the name to use for this model in coupling descriptions.
     */
    public String getName() {
        return "TimeSeriesPlot";
    }
}
