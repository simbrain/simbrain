package org.simbrain.plot

import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.util.Events

open class PlotEvents: Events() {
    val propertyChanged = NoArgEvent()
}

class TimeSeriesEvents: PlotEvents() {
    val scalarTimeSeriesAdded = OneArgEvent<TimeSeriesModel.ScalarTimeSeries>()
    val scalarTimeSeriesRemoved = OneArgEvent<TimeSeriesModel.ScalarTimeSeries>()
}