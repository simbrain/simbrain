package org.simbrain.plot

import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.util.Events

open class PlotEvents: Events() {
    val propertyChanged = NoArgEvent()
}

class TimeSeriesEvents: PlotEvents() {
    val scalarTimeSeriesAdded = AddedEvent<TimeSeriesModel.ScalarTimeSeries>()
    val scalarTimeSeriesRemoved = RemovedEvent<TimeSeriesModel.ScalarTimeSeries>()
}