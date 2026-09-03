/**
 * Tests for the recurrence matrix computation behind [org.simbrain.plot.timeseries.RecurrencePanel]
 * and for persistence of the recurrence settings on [TimeSeriesModel].
 */
package org.simbrain.plot

import org.jfree.chart.axis.AxisSpace
import org.jfree.data.Range
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.plot.timeseries.*
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class RecurrenceTest {

    @Test
    fun `scalar distances match absolute differences with zero diagonal`() {
        val values = doubleArrayOf(0.0, 1.0, 3.0)
        val matrix = computeRecurrenceMatrix(values, embeddingDimension = 1, embeddingDelay = 1)
        assertEquals(3, matrix.size)
        for (i in values.indices) {
            assertEquals(0.0, matrix[i][i], 0.0)
            for (j in values.indices) {
                assertEquals(abs(values[i] - values[j]), matrix[i][j], 1e-12)
                assertEquals(matrix[j][i], matrix[i][j], 0.0)
            }
        }
    }

    @Test
    fun `embedding shortens the matrix and compares delayed samples`() {
        val values = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 10.0)
        val matrix = computeRecurrenceMatrix(values, embeddingDimension = 2, embeddingDelay = 2)
        // 6 samples with dimension 2 and delay 2 leave states anchored at indices 0..3
        assertEquals(4, matrix.size)
        // State 0 is (0,2), state 3 is (3,10): distance sqrt(3^2 + 8^2)
        assertEquals(sqrt(9.0 + 64.0), matrix[0][3], 1e-12)
    }

    @Test
    fun `series shorter than one embedded state yields empty matrix`() {
        val matrix = computeRecurrenceMatrix(doubleArrayOf(1.0, 2.0), embeddingDimension = 3, embeddingDelay = 1)
        assertEquals(0, matrix.size)
    }

    @Test
    fun `recurrence settings survive serialization`() {
        val model = TimeSeriesModel()
        model.recurrenceView = RecurrenceView.BOTH
        model.recurrenceMode = RecurrenceMode.SPECTRUM
        model.recurrenceThreshold = 0.25
        model.recurrenceColorMap = ChartColorMap.GRAYSCALE
        model.recurrenceEmbeddingDimension = 3
        model.recurrenceEmbeddingDelay = 2

        val xml = TimeSeriesPlotComponent.timeSeriesXStream.toXML(model)
        val restored = TimeSeriesPlotComponent.timeSeriesXStream.fromXML(xml) as TimeSeriesModel

        assertEquals(RecurrenceView.BOTH, restored.recurrenceView)
        assertEquals(RecurrenceMode.SPECTRUM, restored.recurrenceMode)
        assertEquals(0.25, restored.recurrenceThreshold, 0.0)
        assertEquals(ChartColorMap.GRAYSCALE, restored.recurrenceColorMap)
        assertEquals(3, restored.recurrenceEmbeddingDimension)
        assertEquals(2, restored.recurrenceEmbeddingDelay)
    }

    @Test
    fun `recurrence defaults to hidden`() {
        assertEquals(RecurrenceView.TIME_SERIES, TimeSeriesModel().recurrenceView)
    }

    @Test
    fun `aligned recurrence panel follows the provided domain range on every refresh`() {
        val model = TimeSeriesModel()
        model.addTimeSeries("series")
        repeat(50) { model.addData(0, it.toDouble(), sin(it / 5.0)) }
        lateinit var panel: RecurrencePanel
        SwingUtilities.invokeAndWait { panel = RecurrencePanel(model, model.timeSeriesList[0]) }
        val domainAxis = panel.chartPanel.chart.xyPlot.domainAxis

        // The panel must pull the range anew on each refresh, not capture it when alignment is set,
        // because the line chart's auto-range moves without firing axis-change events
        val axisSpace = AxisSpace().apply { left = 60.0; right = 8.0 }
        var lineChartRange = Range(0.0, 200.0)
        SwingUtilities.invokeAndWait { panel.domainAlignmentProvider = { DomainAlignment(lineChartRange, axisSpace) } }
        assertEquals(lineChartRange, domainAxis.range)

        lineChartRange = Range(100.0, 300.0)
        SwingUtilities.invokeAndWait { panel.refresh() }
        assertEquals(lineChartRange, domainAxis.range)

        SwingUtilities.invokeAndWait { panel.domainAlignmentProvider = null }
        SwingUtilities.invokeAndWait { panel.refresh() }
        assertEquals(-0.5, domainAxis.range.lowerBound, 1e-12)
        assertEquals(49.5, domainAxis.range.upperBound, 1e-12)
    }
}
