package org.simbrain.plot

import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.util.Events

open class PlotEvents: Events() {
    val propertyChanged = NoArgEvent()
}

class TimeSeriesEvents: PlotEvents() {
    val timeSeriesAdded = OneArgEvent<TimeSeriesModel.TimeSeries>()
    val timeSeriesRemoved = OneArgEvent<TimeSeriesModel.TimeSeries>()
}