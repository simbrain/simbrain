/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.plot.histogram

import org.jfree.data.xy.IntervalXYDataset
import org.simbrain.plot.histogram.OverwritableHistogramDataset.ColoredDataSeries
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.awt.Color

/**
 * Underlying model for the histogram data, in the form of a list of double
 * arrays, one array per histogram. The histograms are represented by different
 * colors in HistogramPanel. The JFreeChart dataset is also stored here.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class HistogramModel(

    /**
     * An list of double arrays to be plotted, one for each series. This is redundant
     * with the dataset object for JFreeChart, but must be kept in case the
     * number of bins is changed.
     */
    var data: MutableList<DoubleArray> = ArrayList(),

    /**
     * An array containing the names of each of the data series.
     */
    var dataNames: MutableList<String> = ArrayList(),

    /**
     * Initial number of bins.
     */
    var bins: Int = 25,
    title: String = "Histogram",
    xAxisName: String = "",
    yAxisName: String = "Count",
    colorPallet: Array<Color>? = null
) : AttributeContainer {

    /**
     * The data set used to generate the histogram.
     */
    private val dataSet = OverwritableHistogramDataset()

    init {
        addDataSources(1)
    }

    /**
     * Add double array data to a specified data series. This is the main method
     * used to dynamically add data when the histogram is used as a plot
     * component.
     *
     * @param index    data index
     * @param histData the data to add at that index
     */
    fun addDataToDataSeries(histData: DoubleArray, index: Int) {
        if (index < data.size) {
            data.removeAt(index)
        }
        data.add(index, histData)
        applyCurrentData()
    }

    /**
     * Called by coupling producers via reflection. For now only only coupling
     * to a single histogram. Later possibly allow coupling to multiple
     * histograms
     *
     * @param histData the array of histogram data
     */
    @Consumable
    fun addData(histData: DoubleArray) {
        addDataToDataSeries(histData, 0)
    }

    fun applyCurrentData() {
        dataSet.resetData(dataNames, data, bins)
    }

    /**
     * Reset the data in the model with provided data and names.
     */
    fun resetData(data: MutableList<DoubleArray>, names: MutableList<String>) {
        this.data = data
        dataNames = names
        dataSet.resetData(dataNames, data, bins)
        applyCurrentData()
    }

    /**
     * Clears the data. Currently just adds a single vector to each data
     * source.
     */
    fun resetData() {
        data.clear()
        dataNames.clear()
        applyCurrentData()
        // TODO: Call an event that initiates GUI refresh
    }

    /**
     * Create specified number of set of data sources. Adds these two existing
     * data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    fun addDataSources(numDataSources: Int) {
        for (i in 0 until numDataSources) {
            addDataSource()
        }
    }

    /**
     * Adds a data source to the chart.
     */
    fun addDataSource() {
        // TODO: Untested; no GUI hook yet.
        data.add(doubleArrayOf(0.0))
        dataNames.add("Hist " + data.size)
        applyCurrentData()
    }

    fun setSeriesColor(name: String?, c: Color?) {
        dataSet.setSeriesColor(name, c)
    }

    /**
     * Return the underlying series data.
     *
     * @return the data
     */
    val seriesData: Collection<ColoredDataSeries>
        get() = dataSet.dataSeries

    fun getDataSet(): IntervalXYDataset {
        return dataSet
    }

    override fun getId(): String {
        return "Histogram"
    }

}