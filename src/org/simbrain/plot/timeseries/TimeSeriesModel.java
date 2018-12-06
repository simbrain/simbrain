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
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import java.util.function.Supplier;

/**
 * Data model for a time series plot.
 */
public class TimeSeriesModel implements AttributeContainer {

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
     * Names for the time series.  Set via coupling events.
     */
    private String[] seriesNames = {};

    /**
     * Time series model constructor.
     */
    public TimeSeriesModel(Supplier<Integer> timeSupplier) {
        this.timeSupplier = timeSupplier;
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
     * Adds a data source to the chart with the specified description.
     */
    public void addTimeSeries(String description) {
        XYSeries xy = new XYSeries(description);
        xy.setMaximumItemCount(maxDataPoints);
        dataset.addSeries(xy);
    }

    /**
     * Add data to this model.
     *
     * @param dataSourceIndex index of data source to use
     * @param time            data for x axis
     * @param value           data for y axis
     * Adds a data source to the chart with the specified description.
     */
    public void addData(int dataSourceIndex, double time, double value) {
        if(dataSourceIndex < dataset.getSeriesCount()) {
            dataset.getSeries(dataSourceIndex).add(time, value);
        }
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable()
    public void addValues(double[] vector) {

        // Take care of size mismatches
        if (vector.length != dataset.getSeriesCount()) {
            clearData();
            for (int i = 0; i < vector.length; i++) {
                if (i < seriesNames.length) {
                    addTimeSeries(seriesNames[i]);
                } else {
                    addTimeSeries("" + i);
                }
            }
        }

        // Write the data
        for (int i = 0; i < vector.length; i++) {
            dataset.getSeries(i).add(timeSupplier.get(), (Double) vector[i]);
        }
    }

    public void setSeriesNames(String[] names) {
        this.seriesNames = names;
    }

    public void setTimeSupplier(Supplier<Integer> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    /**
     * Return the name to use for this model in coupling descriptions.
     */
    public String getName() {
        return "TimeSeriesPlot";
    }

    public boolean isAutoRange() {
        return autoRange;
    }

    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    public double getRangeUpperBound() {
        return rangeUpperBound;
    }

    public void setRangeUpperBound(final double upperBound) {
        this.rangeUpperBound = upperBound;
    }

    public double getRangeLowerBound() {
        return rangeLowerBound;
    }

    public void setRangeLowerBound(final double lowerRangeBoundary) {
        this.rangeLowerBound = lowerRangeBoundary;
    }

    public XYSeriesCollection getDataset() {
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = Utils.getSimbrainXStream();
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
    }
}
