package org.simbrain.plot

import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.util.Events2

open class PlotEvents: Events2() {
    val propertyChanged = NoArgEvent()
}

class TimeSeriesEvents: PlotEvents() {
    val scalarTimeSeriesAdded = AddedEvent<TimeSeriesModel.ScalarTimeSeries>()
    val scalarTimeSeriesRemoved = RemovedEvent<TimeSeriesModel.ScalarTimeSeries>()
    val changeArrayMode = NoArgEvent()
}