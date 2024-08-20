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
import org.simbrain.util.WithXStreamPropertyConverter
import org.simbrain.util.createXStreamPropertyConverter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Workspace
import java.lang.reflect.InvocationTargetException
import javax.swing.SwingUtilities

/**
 * Data model for a time series plot. A time series consumes an array of
 * doubles, with one component for each member of the time series. There is no
 * support currently for representing separate scalar values in a single time
 * series.
 */
class TimeSeriesModel : AttributeContainer, EditableObject {

    @Transient
    lateinit var timeSupplier: () -> Int

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

    var rangeUpperBound by GuiEditable(
        initValue = 1.0,
        label = "Range upper bound",
        description = "Range upper bound in fixed range mode (auto-range turned off)",
        onUpdate = { enableWidget(!widgetValue(TimeSeriesModel::isAutoRange)) },
        order = 20
    )

    var rangeLowerBound by GuiEditable(
        initValue = 0.0,
        label = "Range lower bound",
        description = "Range lower bound in fixed range mode (auto-range turned off)",
        onUpdate = { enableWidget(!widgetValue(TimeSeriesModel::isAutoRange)) },
        order = 30
    )

    var useAutoRangeMinimumSize = false

    var autoRangeMinimumSize by GuiEditable(
        initValue = 1.0,
        label = "Auto Range Minimum Size",
        description = "Minimum size of the range when auto range is used",
        useCheckboxFrom = TimeSeriesModel::useAutoRangeMinimumSize,
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
        order = 70
    )

    /**
     * Names for the time series.  Set via coupling events.
     */
    private val seriesNames = arrayOf<String>()

    /**
     * List of time series objects which can be coupled to.
     */
    val timeSeriesList: MutableList<TimeSeries> = ArrayList()

    @Transient
    var events = TimeSeriesEvents()
        private set

    /**
     * Construct a time series model.
     *
     * @param timeSupplier the supplier for the x-axis of the graph
     */
    init {
        fixedWidth = fixedWidth // Force update by triggering custom setter
    }

    /**
     * Create specified number of data sources.
     *
     * @param numSeries number of data sources to add to the plot.
     */
    fun addTimeSeries(numSeries: Int) {
        for (i in 0 until numSeries) {
            addTimeSeries()
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
        }
    }

    /**
     * Adds a [TimeSeries] with a default description.
     */
    fun addTimeSeries() {
        val description = "Series " + (timeSeriesList.size + 1)
        addTimeSeries(description)
    }

    /**
     * Adds a [TimeSeries] to the chart with a specified
     * description.
     *
     * @param description description for the time series
     * @return a reference to the series, or null if the model is in scalar mode
     */
    fun addTimeSeries(description: String): TimeSeries {
        val sts = TimeSeries(addXYSeries(description))
        timeSeriesList.add(sts)
        events.timeSeriesAdded.fire(sts)
        return sts
    }

    @Consumable
    fun setValues(array: DoubleArray) {
        if (timeSeriesList.isEmpty()) {
            addTimeSeries(array.size)
        }
        var i = 0
        while (i < array.size && i < timeSeriesList.size) {
            timeSeriesList[i].setValue(array[i])
            i++
        }
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
     * Remove all [TimeSeries] objects.
     */
    fun removeAllTimeSeries() {
        for (ts in timeSeriesList) {
            dataset.removeSeries(ts.series)
            events.timeSeriesRemoved.fire(ts)
        }
        timeSeriesList.clear()
    }

    /**
     * Remove a specific scalar time series.
     *
     * @param ts the time series to remove.
     */
    private fun removeTimeSeries(ts: TimeSeries) {
        dataset.removeSeries(ts.series)
        timeSeriesList.remove(ts)
        events.timeSeriesRemoved.fire(ts)
    }

    /**
     * Removes the last data source from the chart.
     */
    fun removeLastTimeSeries() {
        if (timeSeriesList.size > 0) {
            removeTimeSeries(timeSeriesList[timeSeriesList.size - 1])
        }
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
        timeSeriesList.forEach { dataset.addSeries(it.series) }
        return this
    }

    override val id: String
        get() = "Time Series"

    companion object: WithXStreamPropertyConverter {
        override val xStreamPropertyConverter = createXStreamPropertyConverter<TimeSeriesModel>(
            marshal = {
                on(TimeSeriesModel::timeSeriesList) { writer, context ->
                    writer.startNode("timeSeriesList")
                    forEach {
                        writer.startNode("timeSeries")
                        context.convertAnother(it.series)
                        writer.endNode()
                    }
                    writer.endNode()
                }
            }, unmarshal = {
                on("timeSeriesList") { reader, context ->
                    while (reader.hasMoreChildren()) {
                        reader.moveDown()
                        val series = context.convertAnother(reader.value, XYSeries::class.java) as XYSeries
                        withConstructedObject {
                            val sts = TimeSeries(series)
                            timeSeriesList.add(sts)
                            dataset.addSeries(sts.series)
                            events.timeSeriesAdded.fire(sts)
                        }
                        reader.moveUp()
                    }
                }

            }
        )
    }

    /**
     * Encapsulates a single time series for scalar couplings to attach to.
     */
    inner class TimeSeries(
        /**
         * The represented time series
         */
        var series: XYSeries
    ) : AttributeContainer {

        /**
         * Label for the series
         */
        var description: String
            get() = series.description
            set(value) {series.description = value}

        @Consumable
        fun setValue(value: Double) {
            try {
                SwingUtilities.invokeAndWait {
                    series.add(timeSupplier(), value as Number)
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

fun Workspace.createTimeSeriesModel(): TimeSeriesModel {
    return TimeSeriesModel()
}