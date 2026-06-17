package org.simbrain.plot

import org.simbrain.plot.raster.RasterModel
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.util.FlowEvents

open class PlotEvents: FlowEvents() {
    val propertyChanged = NoArgEvent()
}

class TimeSeriesEvents: PlotEvents() {
    val timeSeriesAdded = OneArgEvent<TimeSeriesModel.TimeSeries>()
    val timeSeriesRemoved = OneArgEvent<TimeSeriesModel.TimeSeries>()
}

class RasterPlotEvents: PlotEvents() {
    val rasterConsumerAdded = OneArgEvent<RasterModel.RasterConsumer>()
    val rasterConsumerRemoved = OneArgEvent<RasterModel.RasterConsumer>()
}

