/**
 * Snapshot of [org.simbrain.plot.timeseries.TimeSeriesPlotPanel]'s Swing legend strip: per-series
 * color swatch, name, and the muted per-series remove control that replaced the delete-last button.
 * Hover styling can't be captured here; only the resting state is the subject.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import java.awt.Component
import kotlin.math.sin

class TimeSeriesLegendSnapshot : UiSnapshotDef {
    override val name = "time_series_legend"

    override fun build(): Component {
        val model = TimeSeriesModel()
        model.timeSupplier = { 0 }
        model.addTimeSeries("Neuron 1")
        model.addTimeSeries("Neuron 2")
        model.addTimeSeries("Neuron 3")
        for (t in 0..80) {
            model.addData(0, t.toDouble(), sin(t / 8.0))
            model.addData(1, t.toDouble(), sin(t / 8.0 + 1.5) * 0.7)
            model.addData(2, t.toDouble(), sin(t / 8.0 + 3.0) * 0.4)
        }
        return TimeSeriesPlotPanel(model)
    }
}
