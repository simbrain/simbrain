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
package org.simbrain.plot.timeseries

import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.plot.TimeSeriesEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.lang.reflect.InvocationTargetException
import java.util.function.Consumer
import java.util.function.Supplier
import javax.swing.SwingUtilities

/**
 * Data model for a time series plot. A time series consumes an array of
 * doubles, with one component for each member of the time series. There is no
 * support currently for representing separate scalar values in a single time
 * series.
 */
class TimeSeriesModel(
    /**
     * Lambda to supply time to the time series model.
     */
    @field:Transient private var timeSupplier: Supplier<Int>
) : AttributeContainer, EditableObject {

    /**
     * Time Series Data.
     */
    @Transient
    var dataset = XYSeriesCollection()
        private set

    @UserParameter(
        label = "Auto Range", description = "If true, automatically adjusts the range of the time series data " +
                "based on the maximum and minimum values present at a given time", order = 10
    )
    var isAutoRange = true

    /**
     * When this is true the chart uses a fixed range, even though auto-range is on
     * (this is true when the time series max value < fixedRangeThreshold)
     */
    var isUseFixedRangeWindow = false
        private set(disableAutoRange) {
            val oldValue = isUseFixedRangeWindow
            field = disableAutoRange
            if (oldValue != disableAutoRange) {
                events.propertyChanged.fireAndBlock()
            }
        }

    var fixedRangeThreshold by GuiEditable(
        initValue = 0.0,
        label = "Fixed range threshold",
        description = "When the time series values fall below this threshold a fixed range is used." +
                " (use 0 to effectively disable this)",
        conditionallyEnabledBy = TimeSeriesModel::isAutoRange,
        order = 20
    )

    var rangeUpperBound by GuiEditable(
        initValue = 1.0,
        label = "Range upper bound",
        description = "Range upper bound in fixed range mode (auto-range turned off)",
        conditionallyEnabledBy = TimeSeriesModel::isUseFixedRangeWindow,
        order = 30
    )

    var rangeLowerBound by GuiEditable(
        initValue = 0.0,
        label = "Range lower bound",
        description = "Range lower bound in fixed range mode (auto-range turned off)",
        conditionallyEnabledBy = TimeSeriesModel::isUseFixedRangeWindow,
        order = 40
    )

    @UserParameter(
        label = "Fixed Width", description = "If true, the time series window never extends beyond a fixed with",
        order = 60
    )
    var fixedWidth = false
        set(value) {
            field = value
            if (value) {
                for (s in dataset.series) {
                    (s as XYSeries?)!!.maximumItemCount = windowSize
                }
            } else {
                for (s in dataset.series) {
                    (s as XYSeries?)!!.maximumItemCount = Int.MAX_VALUE
                }
            }
        }

    var windowSize by GuiEditable(
        initValue = 100,
        label = "Window size",
        description = "Size of window when fixed width is used.",
        conditionallyEnabledBy = TimeSeriesModel::fixedWidth,
        order = 40
    )

    /**
     * Names for the time series.  Set via coupling events.
     */
    private val seriesNames = arrayOf<String>()

    /**
     * List of time series objects which can be coupled to.
     */
    val timeSeriesList: MutableList<ScalarTimeSeries> = ArrayList()

    @Transient
    var events = TimeSeriesEvents()
        private set

    /**
     * Construct a time series model.
     *
     * @param timeSupplier the supplier for the x-axis of the graph
     */
    init {
        addScalarTimeSeries(3)
        fixedWidth = fixedWidth // Force update by triggering custom setter
    }

    /**
     * Create specified number of data sources.
     *
     * @param numSeries number of data sources to add to the plot.
     */
    fun addScalarTimeSeries(numSeries: Int) {
        for (i in 0 until numSeries) {
            addScalarTimeSeries()
        }
    }

    /**
     * Clears the plot.
     */
    fun clearData() {
        val seriesCount = dataset.seriesCount
        var i = 0
        while (seriesCount > i) {
            dataset.getSeries(i).clear()
            ++i
        }
    }

    /**
     * Add scalar data to a specified time series. Called by scripts.
     *
     * @param seriesIndex index of data source to use
     * @param time        data for x axis
     * @param value       data for y axis Adds a data source to the chart with
     * the specified description.
     */
    fun addData(seriesIndex: Int, time: Double, value: Double) {
        if (seriesIndex < dataset.seriesCount) {
            val currentSeries = dataset.getSeries(seriesIndex)
            currentSeries.add(time, value)
            revalidateUseFixedRangeWindow(currentSeries.maxY)
        }
    }

    /**
     * Adds a [ScalarTimeSeries] with a default description.
     */
    fun addScalarTimeSeries() {
        val description = "Series " + (timeSeriesList.size + 1)
        addScalarTimeSeries(description)
    }

    /**
     * Adds a [ScalarTimeSeries] to the chart with a specified
     * description.
     *
     * @param description description for the time series
     * @return a reference to the series, or null if the model is in scalar mode
     */
    fun addScalarTimeSeries(description: String): ScalarTimeSeries {
        val sts = ScalarTimeSeries(addXYSeries(description))
        timeSeriesList.add(sts)
        events.scalarTimeSeriesAdded.fireAndBlock(sts)
        return sts
    }

    @Consumable
    fun setValues(array: DoubleArray) {
        var i = 0
        while (i < array.size && i < timeSeriesList.size) {
            timeSeriesList[i].setValue(array[i])
            i++
        }
        revalidateUseFixedRangeWindow(dataset.getRangeUpperBound(false))
    }

    /**
     * Adds an xy series to the chart with the specified description.
     */
    private fun addXYSeries(description: String): XYSeries {
        val xy = XYSeries(description)
        xy.maximumItemCount = windowSize
        xy.description = description
        dataset.addSeries(xy)
        return xy
    }

    /**
     * Remove all [ScalarTimeSeries] objects.
     */
    fun removeAllScalarTimeSeries() {
        for (ts in timeSeriesList) {
            dataset.removeSeries(ts.series)
            events.scalarTimeSeriesRemoved.fireAndBlock(ts)
        }
        timeSeriesList.clear()
    }

    /**
     * Remove a specific scalar time series.
     *
     * @param ts the time series to remove.
     */
    private fun removeTimeSeries(ts: ScalarTimeSeries) {
        dataset.removeSeries(ts.series)
        timeSeriesList.remove(ts)
        events.scalarTimeSeriesRemoved.fireAndBlock(ts)
    }

    /**
     * Removes the last data source from the chart.
     */
    fun removeLastScalarTimeSeries() {
        if (timeSeriesList.size > 0) {
            removeTimeSeries(timeSeriesList[timeSeriesList.size - 1])
        }
    }

    fun setTimeSupplier(timeSupplier: Supplier<Int>) {
        this.timeSupplier = timeSupplier
    }

    private fun revalidateUseFixedRangeWindow(maxValue: Double) {
        isUseFixedRangeWindow = fixedRangeThreshold != 0.0 && maxValue < fixedRangeThreshold
    }

    /**
     * The name to used in coupling descriptions.
     */
    override val name: String
        get() = "TimeSeriesPlot"

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    private fun readResolve(): Any {
        events = TimeSeriesEvents()
        dataset = XYSeriesCollection()
        timeSeriesList.forEach(Consumer { ts: ScalarTimeSeries -> dataset.addSeries(ts.series) })
        return this
    }

    override val id: String
        get() = "Time Series"

    /**
     * Encapsulates a single time series for scalar couplings to attach to.
     */
    inner class ScalarTimeSeries(
        /**
         * The represented time series
         */
        var series: XYSeries
    ) : AttributeContainer {

        /**
         * Get the description.
         */
        val description: String
            get() = series.description

        @Consumable
        fun setValue(value: Double) {
            try {
                SwingUtilities.invokeAndWait {
                    series.add(timeSupplier.get(), value as Number)
                    revalidateUseFixedRangeWindow(series.maxY)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }

        override val id: String
            get() = description

    }
}