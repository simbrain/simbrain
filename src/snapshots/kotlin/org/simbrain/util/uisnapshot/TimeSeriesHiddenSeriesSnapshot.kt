/**
 * Snapshot of [org.simbrain.plot.timeseries.TimeSeriesPlotPanel] with one series hidden: its legend
 * entry renders with a hollow swatch and muted name, its line is absent from the chart, and the
 * auto-range ignores it, so the visible small-amplitude series fill the plot despite the hidden
 * series' much larger values.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import java.awt.Component
import kotlin.math.sin

class TimeSeriesHiddenSeriesSnapshot : UiSnapshotDef {
    override val name = "time_series_hidden_series"

    override fun build(): Component {
        val model = TimeSeriesModel()
        model.timeSupplier = { 0 }
        model.addTimeSeries("Neuron 1")
        model.addTimeSeries("Neuron 2")
        model.addTimeSeries("Neuron 3")
        for (t in 0..80) {
            model.addData(0, t.toDouble(), sin(t / 8.0))
            model.addData(1, t.toDouble(), sin(t / 8.0 + 1.5) * 5.0)
            model.addData(2, t.toDouble(), sin(t / 8.0 + 3.0) * 0.4)
        }
        model.timeSeriesList[1].visible = false
        return TimeSeriesPlotPanel(model)
    }
}
