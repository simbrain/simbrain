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
package org.simbrain.plot.raster

import com.thoughtworks.xstream.XStream
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.lang.reflect.InvocationTargetException
import java.util.function.Supplier
import javax.swing.SwingUtilities

/**
 * Data model for a raster plot.
 */
class RasterModel(timeSupplier: Supplier<Int>? = null) : EditableObject {

    /**
     * Lambda to supply time to the time series model.
     */
    @Transient
    lateinit var timeSupplier: Supplier<Int>

    /**
     * Raster Data.
     */
    val dataset: XYSeriesCollection = XYSeriesCollection()

    /**
     * List of [RasterConsumer]'s that consume raster data.
     */
    @Transient
    val rasterConsumerList = dataset.series.mapIndexed { index, _ -> RasterConsumer(index) }.toMutableList()

    @UserParameter(
        label = "Dot Size",
        description = "Size of dots in chart",
        order = 5)
    var dotSize: Int = 4

    var windowSize: Int by GuiEditable(
        initValue = 100,
        label = "Window Size",
        description = "How many time points can be contained in the window",
        conditionallyEnabledBy = RasterModel::isFixedWidth,
        order = 10
    )

    /**
     * Whether this chart if fixed width or not.
     */
    @UserParameter(
        label = "Fixed width",
        description = "If true, the raster window never extends beyond a fixed with",
        order = 30
    )
    var isFixedWidth: Boolean = true

    @UserParameter(
        label = "Spike Threshold",
        description = "For nonspiking neurons activation above this is taken to be a spike",
        order = 40
    )
    var spikeThreshold: Double = 0.5

    /**
     * Raster series model constructor.
     */
    init {
        addDataSources(INITIAL_DATA_SOURCES)
        if (timeSupplier != null) {
            this.timeSupplier = timeSupplier
        }
    }

    /**
     * Create specified number of set of data sources. Adds these two existing
     * data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    fun addDataSources(numDataSources: Int, names: List<String>? = null) {
        for (i in 0 until numDataSources) {
            addDataSource(names?.get(i) ?: (dataset.seriesCount + 1).toString())
        }
    }

    /**
     * Removes a data source from the chart.
     */
    fun removeDataSource() {
        val lastSeriesIndex = dataset.seriesCount - 1
        if (lastSeriesIndex >= 0) {
            dataset.removeSeries(lastSeriesIndex)
            rasterConsumerList.removeAt(lastSeriesIndex)
        }
    }

    /**
     * Adds a data source to the chart.
     */
    @JvmOverloads
    fun addDataSource(name: String = (dataset.seriesCount + 1).toString()) {
        val currentSize = dataset.seriesCount
        dataset.addSeries(XYSeries(name))
        rasterConsumerList.add(RasterConsumer(currentSize))
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
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    private fun readResolve(): Any {
        return this
    }


    /**
     * Objects that represent separate sets of raster points, shown in a different color in the
     * chart.
     */
    inner class RasterConsumer internal constructor(index: Int) : AttributeContainer {
        /**
         * Index of this consumer in an [XYSeriesCollection]
         */
        var index: Int = 0

        init {
            this.index = index
        }

        /**
         * Plot an array of values as a vertical bar in a raster plot. Each component of the array is associated with one row of the plot.
         * Canonically used to display spiking data, represented with binary vectors. If real-values (e.g. activations) are sent in, then values above a threshold (default .5) are interpreted as spikes
         * <br></br>
         * Example 1: [0, 1, 0, 0 , 1] would show 2 dots vertically at the 2nd and 5th position at the current time
         * <br></br>
         * Example 2: [0.0, 0.6, -0.3, 0.0, 1.0] would show 2 dots vertically at the 2nd and 5th position at the current time
         */
        @Consumable
        fun setValues(values: DoubleArray) {
            try {
                SwingUtilities.invokeAndWait {
                    var udpated = false
                    var i = 0
                    val n = values.size
                    while (i < n) {
                        if (values[i] >= spikeThreshold) {
                            dataset.getSeries(index).add(timeSupplier.get(), i)
                            udpated = true
                        }
                        i++
                    }
                    if (!udpated) {
                        dataset.getSeries(index).add(timeSupplier.get(), null)
                    }
                }
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            }
        }

        override val id: String
            get() = "Raster " + (index + 1)
    }

    companion object {
        /**
         * Default number of data sources for plot initialization.
         */
        private const val INITIAL_DATA_SOURCES = 1

        @JvmStatic
        val xStream: XStream
            /**
             * Returns a properly initialized xstream object.
             *
             * @return the XStream object
             */
            get() {
                val xstream = getSimbrainXStream()
                return xstream
            }
    }
}
