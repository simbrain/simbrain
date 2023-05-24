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
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * Should the range automatically change to reflect the data.
     */
    @UserParameter(label = "Auto Range", order = 3)
    private boolean autoRange = true;

    /**
     * Upper bound of the chart range.
     */
    @UserParameter(label = "Range upper Bound", order = 10)
    private double rangeUpperBound = 1;

    /**
     * Lower bound of the chart range.
     */
    @UserParameter(label = "Range Lower Bound", order = 20)
    private double rangeLowerBound = 0;

    /**
     * Whether this chart if fixed width or not.
     */
    @UserParameter(label = "Fixed Width", description = "If set, the time series window never " +
            "extends beyond a fixed with", useSetter = true, order = 50)
    private boolean fixedWidth = false;

    /**
     * Size of window when fixed width is being used.
     */
    @UserParameter(label = "Window Size", description = "Number of time points to restrict window to, " +
            "when fixedWidth is turned on", minimumValue = 10, useSetter = true, increment = 10, order = 60)
    private int windowSize = 100;

    /**
     * Names for the time series.  Set via coupling events.
     */
    private String[] seriesNames = {};

    /**
     * List of time series objects which can be coupled to.
     */
    private List<ScalarTimeSeries> timeSeriesList = new ArrayList<ScalarTimeSeries>();

    /**
     * Support for property change events.
     */
    private transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * If true, the plot is receiving an array coupling.  If false, scalar
     * couplings are being used, via {@link ScalarTimeSeries} objects. When a
     * time series is added or removed (e.g. from the GUI or a script) array
     * mode ceases and array couplings are removed. When an array coupling is
     * created, all time series objects are removed and array mode is true.
     */
    private boolean isArrayMode = false;

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
            dataset.getSeries(seriesIndex).add(time, value);
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
        if (isArrayMode) {
            return null;
        }
        ScalarTimeSeries sts = new ScalarTimeSeries(addXYSeries(description));
        timeSeriesList.add(sts);
        changeSupport.firePropertyChange("scalarTimeSeriesAdded", null, sts);
        return sts;
    }

    /**
     * Called by coupling producers via reflection.  Each component of a vector
     * is applied to a separate time series.
     */
    @Consumable()
    public void addValues(double[] vector) {

        // If there is a size mismatch (for example, after removing neurons from
        // a neuron group sending activations), clear and start over.
        // Resets labels for all time series
        if (vector.length != dataset.getSeriesCount()) {
            dataset.removeAllSeries();
            timeSeriesList.clear();
            for (int i = 0; i < vector.length; i++) {
                if (i < seriesNames.length) {
                    addXYSeries(seriesNames[i]);
                } else {
                    addXYSeries("" + i);
                }
            }
        }

        // Write the data
        for (int i = 0; i < vector.length; i++) {
            dataset.getSeries(i).add(timeSupplier.get(), (Double) vector[i]);
        }
    }

    /**
     * Initialize array mode.
     *
     * @param names names for new series
     */
    public void initializeArrayMode(String[] names) {
        isArrayMode = true;
        dataset.removeAllSeries();
        this.seriesNames = names;
        int i = 0;
        for (String name : names) {
            addXYSeries(names[i] + 1);
            i++;
        }
    }

    /**
     * Turn off array mode. Remove all scalar time series.
     */
    public void setArrayMode(boolean isArrayMode) {
        this.isArrayMode = isArrayMode;
        dataset.removeAllSeries();
        removeAllScalarTimeSeries();
        changeSupport.firePropertyChange("changeArrayMode", null, null);
        if (isArrayMode) {
            // No action
        } else {
            addScalarTimeSeries(3); // Add default time series
            // If a scalar coupling is added when an array coupling is in place,
            // remove all lingering aspects of the array coupling
        }

    }

    public boolean isArrayMode() {
        return isArrayMode;
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
            changeSupport.firePropertyChange("scalarTimeSeriesRemoved", ts, null);
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
        changeSupport.firePropertyChange("scalarTimeSeriesRemoved", ts, null);
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
        changeSupport = new PropertyChangeSupport(this);
        dataset = new XYSeriesCollection();
        timeSeriesList.forEach(ts -> dataset.addSeries(ts.series));
        return this;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
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
                SwingUtilities.invokeAndWait(() -> series.add(timeSupplier.get(), (Number) value));
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