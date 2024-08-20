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

import com.thoughtworks.xstream.XStream
import org.simbrain.plot.XYSeriesConverter
import org.simbrain.plot.timeseries.TimeSeriesModel.TimeSeries
import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.couplings.Coupling
import java.io.InputStream
import java.io.OutputStream

/**
 * Represents time series data.
 */

class TimeSeriesPlotComponent @JvmOverloads constructor(name: String, val model: TimeSeriesModel = TimeSeriesModel()) : WorkspaceComponent(name) {

    override var workspace: Workspace
        get() = super.workspace
        set(workspace) {
            // Workspace object is not available in the constructor.
            super.workspace = workspace

            workspace.couplingManager.events.couplingAdded.on { c: Coupling ->
                // A new array coupling is being added to this time series
                if (c.consumer.baseObject === model) {
                    // Initialize series with provided names, e.g neuron labels
                    val labels = c.producer.labelArray
                    if (labels != null) {
                        model.removeAllTimeSeries()
                        for (i in labels.indices) {
                            model.addTimeSeries(labels[i])
                        }
                    }
                }
            }

            model.events.timeSeriesAdded.on { addedContainer: TimeSeries ->
                this.fireAttributeContainerAdded(addedContainer)
            }

            model.events.timeSeriesRemoved.on { removedContainer: TimeSeries ->
                this.fireAttributeContainerRemoved(removedContainer)
            }
        }

    override val attributeContainers: List<AttributeContainer>
        get() {
            val containers: MutableList<AttributeContainer> = ArrayList()
            containers.add(model)
            containers.addAll(model.timeSeriesList)
            return containers
        }

    fun addTimeSeries(name: String) = model.addTimeSeries(name)

    override fun save(output: OutputStream, format: String?) {
        timeSeriesXStream.toXML(model, output)
    }

    override fun hasChangedSinceLastSave(): Boolean {
        return false
    }

    override val xml: String
        get() = timeSeriesXStream.toXML(model)

    init {
        model.timeSupplier = { workspace.time }
    }

    companion object {
        /**
         * Opens a saved time series plot.
         *
         * @param input  stream
         * @param name   name of file
         * @param format format
         * @return bar chart component to be opened
         */
        fun open(input: InputStream, name: String, format: String?): TimeSeriesPlotComponent {
            val dataModel = timeSeriesXStream.fromXML(input) as TimeSeriesModel
            return TimeSeriesPlotComponent(name, dataModel)
        }

        val timeSeriesXStream: XStream
            get() {
                val xstream = getSimbrainXStream()
                xstream.registerConverter(DoubleArrayConverter())
                xstream.registerConverter(XYSeriesConverter())
                return xstream
            }
    }
}
