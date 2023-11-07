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

import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.plot.TimeSeriesEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Data model for a time series plot. A time series consumes an array of
 * doubles, with one component for each member of the time series. There is no
 * support currently for representing separate scalar values in a single time
 * series.
 */
public class TimeSeriesModel implements AttributeContainer, EditableObject {

    /**
     * Time Series Data.
     */
    private transient XYSeriesCollection dataset = new XYSeriesCollection();

    /**
     * Lambda to supply time to the time series model.
     */
    private transient Supplier<Integer> timeSupplier;

    @UserParameter(label = "Auto Range", description = "If true, automatically adjusts the range of the time series data " +
            "based on the maximum and minimum values present at a given time",  order = 10)
    private boolean autoRange = true;

    /**
     * When this is true the chart uses a fixed range, even though auto-range is on
     * (this is true when the time series max value < fixedRangeThreshold)
     */
    private boolean useFixedRangeWindow = false;

    @UserParameter(label = "Fixed range threshold", description = "When the time series values fall below this " +
            "threshold a fixed range is used. (use 0 to effectively disable this)", conditionalEnablingMethod = "usesAutoRange", order = 20)
    private double fixedRangeThreshold = 0;

    @UserParameter(label = "Range upper bound", description = "Range upper bound in fixed range mode (auto-range " +
            "turned off)", conditionalEnablingMethod = "usesFixedRange", order = 30)
    private double rangeUpperBound = 1;

    @UserParameter(label = "Range lower bound", description = "Range lower bound in fixed range mode (auto-range " +
            "turned off)", conditionalEnablingMethod = "usesFixedRange", order = 40)
    private double rangeLowerBound = 0;

    @UserParameter(label = "Fixed Width", description = "If true, the time series window never " +
            "extends beyond a fixed with", order = 60)
    private boolean fixedWidth = false;

    /**
     * Size of window when fixed width is being used.
     */
    @UserParameter(label = "Window Size", description = "Number of time points to restrict window to, " +
            "when fixedWidth is turned on", minimumValue = 10, conditionalEnablingMethod = "usesFixedWidth", increment = 10, order = 70)
    private int windowSize = 100;

    /**
     * Names for the time series.  Set via coupling events.
     */
    private String[] seriesNames = {};

    /**
     * List of time series objects which can be coupled to.
     */
    private List<ScalarTimeSeries> timeSeriesList = new ArrayList<ScalarTimeSeries>();

    private transient TimeSeriesEvents events = new TimeSeriesEvents();

    /**
     * Construct a time series model.
     *
     * @param timeSupplier the supplier for the x-axis of the graph
     */
    public TimeSeriesModel(Supplier<Integer> timeSupplier) {
        this.timeSupplier = timeSupplier;
        addScalarTimeSeries(3);
        setFixedWidth(fixedWidth); // Force update
    }

    /**
     * Create specified number of data sources.
     *
     * @param numSeries number of data sources to add to the plot.
     */
    public void addScalarTimeSeries(int numSeries) {
        for (int i = 0; i < numSeries; i++) {
            addScalarTimeSeries();
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
     * Add scalar data to a specified time series. Called by scripts.
     *
     * @param seriesIndex index of data source to use
     * @param time        data for x axis
     * @param value       data for y axis Adds a data source to the chart with
     *                    the specified description.
     */
    public void addData(int seriesIndex, double time, double value) {
        if (seriesIndex < dataset.getSeriesCount()) {
            var currentSeries = dataset.getSeries(seriesIndex);
            currentSeries.add(time, value);
            revalidateUseFixedRangeWindow(currentSeries.getMaxY());
        }
    }

    /**
     * Adds a {@link ScalarTimeSeries} with a default description.
     */
    public void addScalarTimeSeries() {
        String description = "Series " + (timeSeriesList.size() + 1);
        addScalarTimeSeries(description);
    }

    /**
     * Adds a {@link ScalarTimeSeries} to the chart with a specified
     * description.
     *
     * @param description description for the time series
     * @return a reference to the series, or null if the model is in scalar mode
     */
    public ScalarTimeSeries addScalarTimeSeries(String description) {
        ScalarTimeSeries sts = new ScalarTimeSeries(addXYSeries(description));
        timeSeriesList.add(sts);
        events.getScalarTimeSeriesAdded().fireAndBlock(sts);
        return sts;
    }

    @Consumable()
    public void setValues(double[] array) {
        for (int i = 0; i < array.length && i < timeSeriesList.size() ; i++) {
            timeSeriesList.get(i).setValue(array[i]);
        }
        revalidateUseFixedRangeWindow(dataset.getRangeUpperBound(false));
    }

    /**
     * Adds an xy series to the chart with the specified description.
     */
    private XYSeries addXYSeries(String description) {
        XYSeries xy = new XYSeries(description);
        xy.setMaximumItemCount(windowSize);
        xy.setDescription(description);
        dataset.addSeries(xy);
        return xy;
    }

    /**
     * Remove all {@link ScalarTimeSeries} objects.
     */
    public void removeAllScalarTimeSeries() {
        for (ScalarTimeSeries ts : timeSeriesList) {
            dataset.removeSeries(ts.getSeries());
            events.getScalarTimeSeriesRemoved().fireAndBlock(ts);
        }
        timeSeriesList.clear();
    }

    /**
     * Remove a specific scalar time series.
     *
     * @param ts the time series to remove.
     */
    private void removeTimeSeries(ScalarTimeSeries ts) {
        dataset.removeSeries(ts.getSeries());
        timeSeriesList.remove(ts);
        events.getScalarTimeSeriesRemoved().fireAndBlock(ts);
    }

    /**
     * Removes the last data source from the chart.
     */
    public void removeLastScalarTimeSeries() {
        if (timeSeriesList.size() > 0) {
            removeTimeSeries(timeSeriesList.get(timeSeriesList.size() - 1));
        }
    }

    /**
     * Set the maximum number of data points to (corresponds to time steps) to
     * plot for each time series.
     */
    public void setWindowSize(int value) {
        windowSize = value;
    }

    public XYSeriesCollection getDataset() {
        return dataset;
    }

    public List<ScalarTimeSeries> getTimeSeriesList() {
        return timeSeriesList;
    }

    public void setTimeSupplier(Supplier<Integer> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    public boolean isAutoRange() {
        return autoRange;
    }

    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    public boolean isUseFixedRangeWindow() {
        return useFixedRangeWindow;
    }

    private void setUseFixedRangeWindow(final boolean disableAutoRange) {
        var oldValue = this.useFixedRangeWindow;
        this.useFixedRangeWindow = disableAutoRange;
        if (oldValue != disableAutoRange) {
            events.getPropertyChanged().fireAndBlock();
        }
    }

    private void revalidateUseFixedRangeWindow(double maxValue) {
        setUseFixedRangeWindow(fixedRangeThreshold != 0 && maxValue < fixedRangeThreshold);
    }

    public double getFixedRangeThreshold() {
        return fixedRangeThreshold;
    }

    public void setFixedRangeThreshold(double fixedRangeThreshold) {
        this.fixedRangeThreshold = fixedRangeThreshold;
    }

    public Function<Map<String, Object>, Boolean> usesFixedRange() {
        return (map) -> !(Boolean) map.get("Auto Range");
    }

    public Function<Map<String, Object>, Boolean> usesAutoRange() {
        return (map) -> (Boolean) map.get("Auto Range");
    }

    public Function<Map<String, Object>, Boolean> usesFixedWidth() {
        return (map) -> (Boolean) map.get("Fixed Width");
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

    public boolean isFixedWidth() {
        return fixedWidth;
    }

    public void setFixedWidth(boolean fixedWidth) {
        this.fixedWidth = fixedWidth;
        if(fixedWidth) {
            for (Object s : dataset.getSeries()) {
                ((XYSeries) s).setMaximumItemCount(windowSize);
            }
        } else {
            for (Object s : dataset.getSeries()) {
                ((XYSeries) s).setMaximumItemCount(Integer.MAX_VALUE);
            }
        }
    }

    /**
     * The name to used in coupling descriptions.
     */
    public String getName() {
        return "TimeSeriesPlot";
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private Object readResolve() {
        events = new TimeSeriesEvents();
        dataset = new XYSeriesCollection();
        timeSeriesList.forEach(ts -> dataset.addSeries(ts.series));
        return this;
    }


    public TimeSeriesEvents getEvents() {
        return events;
    }

    @Nullable
    @Override
    public String getId() {
        return "Time Series";
    }

    /**
     * Encapsulates a single time series for scalar couplings to attach to.
     */
    public class ScalarTimeSeries implements AttributeContainer {

        /**
         * The represented time series
         */
        XYSeries series;

        /**
         * Construct the time series.
         */
        public ScalarTimeSeries(XYSeries xy) {
            series = xy;
        }

        public XYSeries getSeries() {
            return series;
        }

        /**
         * Get the description.
         */
        public String getDescription() {
            return series.getDescription();
        }

        @Consumable()
        public void setValue(double value) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    series.add(timeSupplier.get(), (Number) value);
                    revalidateUseFixedRangeWindow(series.getMaxY());
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getId() {
            return getDescription();
        }
    }
}