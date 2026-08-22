package org.simbrain.util.uisnapshot

import org.simbrain.plot.ChartColorMap
import org.simbrain.plot.timeseries.RecurrenceMode
import org.simbrain.plot.timeseries.RecurrencePanel
import org.simbrain.plot.timeseries.TimeSeriesModel
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.sin

/**
 * Recurrence plots in both modes over a periodic and a chaotic series, so the threshold dot pattern,
 * the spectrum colorbar, and the embedded variant can all be inspected against light and dark. A sine
 * should show clean diagonal stripes; the logistic map should show broken, irregular texture.
 */
class RecurrencePlotSnapshot : UiSnapshotDef {
    override val name = "recurrence_plot"

    private fun sineModel() = TimeSeriesModel().apply {
        addTimeSeries("Sine")
        repeat(120) { addData(0, it.toDouble(), sin(it / 6.0)) }
    }

    private fun logisticModel() = TimeSeriesModel().apply {
        addTimeSeries("Logistic")
        var x = 0.4
        repeat(120) {
            addData(0, it.toDouble(), x)
            x = 3.99 * x * (1 - x)
        }
    }

    override fun build(): Component {
        lateinit var panel: JPanel
        SwingUtilities.invokeAndWait {
            panel = JPanel(GridLayout(2, 2, 8, 8)).apply {
                preferredSize = Dimension(1000, 760)

                val thresholdSine = sineModel().apply { recurrenceMode = RecurrenceMode.THRESHOLD }
                add(RecurrencePanel(thresholdSine, thresholdSine.timeSeriesList[0]))

                val spectrumSine = sineModel().apply {
                    recurrenceMode = RecurrenceMode.SPECTRUM
                    recurrenceColorMap = ChartColorMap.JET
                }
                add(RecurrencePanel(spectrumSine, spectrumSine.timeSeriesList[0]))

                val thresholdLogistic = logisticModel().apply {
                    recurrenceMode = RecurrenceMode.THRESHOLD
                    recurrenceEmbeddingDimension = 2
                }
                add(RecurrencePanel(thresholdLogistic, thresholdLogistic.timeSeriesList[0]))

                val spectrumLogistic = logisticModel().apply {
                    recurrenceMode = RecurrenceMode.SPECTRUM
                    recurrenceColorMap = ChartColorMap.GRAYSCALE
                }
                add(RecurrencePanel(spectrumLogistic, spectrumLogistic.timeSeriesList[0]))
            }
        }
        return panel
    }
}
