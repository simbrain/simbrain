/**
 * Tests for [AdaptiveChartRepainter]'s frame loop: change bursts coalesce into one frame, a finished
 * draw schedules a follow-up only when something changed during it, and stock repainting is restored
 * on uninstall.
 */
package org.simbrain.plot

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.event.ChartProgressEvent
import org.jfree.data.xy.XYSeriesCollection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.swing.SwingUtilities

class AdaptiveChartRepaintTest {

    private class CountingChartPanel(chart: JFreeChart) : ChartPanel(chart) {
        var repaints = 0
        override fun repaint() {
            repaints++
            super.repaint()
        }

        override fun isShowing() = true
    }

    private fun newChart(): JFreeChart =
        ChartFactory.createXYLineChart("", "x", "y", XYSeriesCollection())

    @Test
    fun `a burst of chart changes requests a single frame`() {
        lateinit var panel: CountingChartPanel
        SwingUtilities.invokeAndWait {
            val chart = newChart()
            panel = CountingChartPanel(chart)
            AdaptiveChartRepainter(panel, minFrameMillis = 10).install()
            panel.repaints = 0
            repeat(5) { chart.fireChartChanged() }
            assertEquals(1, panel.repaints)
        }
    }

    @Test
    fun `changes landing before the frame finishes drawing schedule one follow-up frame`() {
        lateinit var panel: CountingChartPanel
        lateinit var repainter: AdaptiveChartRepainter
        lateinit var chart: JFreeChart
        SwingUtilities.invokeAndWait {
            chart = newChart()
            panel = CountingChartPanel(chart)
            repainter = AdaptiveChartRepainter(panel, minFrameMillis = 10)
            repainter.install()
            panel.repaints = 0
            chart.fireChartChanged()
            assertEquals(1, panel.repaints)
            // More data arrives while the requested frame is still drawing
            repainter.chartProgress(ChartProgressEvent(chart, chart, ChartProgressEvent.DRAWING_STARTED, 0))
            chart.fireChartChanged()
            chart.fireChartChanged()
            assertEquals(1, panel.repaints)
            repainter.chartProgress(ChartProgressEvent(chart, chart, ChartProgressEvent.DRAWING_FINISHED, 100))
        }
        // The follow-up frame lands after the minimum interval elapses
        Thread.sleep(100)
        SwingUtilities.invokeAndWait { assertEquals(2, panel.repaints) }

        // A clean draw with no changes during it schedules nothing further
        SwingUtilities.invokeAndWait {
            repainter.chartProgress(ChartProgressEvent(chart, chart, ChartProgressEvent.DRAWING_STARTED, 0))
            repainter.chartProgress(ChartProgressEvent(chart, chart, ChartProgressEvent.DRAWING_FINISHED, 100))
        }
        Thread.sleep(100)
        SwingUtilities.invokeAndWait { assertEquals(2, panel.repaints) }
    }

    @Test
    fun `a draw that never finishes is recovered by the stale-latch watchdog`() {
        lateinit var panel: CountingChartPanel
        lateinit var repainter: AdaptiveChartRepainter
        lateinit var chart: JFreeChart
        SwingUtilities.invokeAndWait {
            chart = newChart()
            panel = CountingChartPanel(chart)
            repainter = AdaptiveChartRepainter(panel, minFrameMillis = 1, staleLatchMillis = 1)
            repainter.install()
            panel.repaints = 0
            chart.fireChartChanged()
            assertEquals(1, panel.repaints)
            // The draw starts and then throws: DRAWING_FINISHED never arrives
            repainter.chartProgress(ChartProgressEvent(chart, chart, ChartProgressEvent.DRAWING_STARTED, 0))
        }
        Thread.sleep(20)
        // A later change finds the latches stale and reschedules instead of wedging forever
        SwingUtilities.invokeAndWait {
            chart.fireChartChanged()
            assertEquals(2, panel.repaints)
        }
    }

    @Test
    fun `uninstall restores repaint per notification`() {
        SwingUtilities.invokeAndWait {
            val chart = newChart()
            val panel = CountingChartPanel(chart)
            val repainter = AdaptiveChartRepainter(panel, minFrameMillis = 10)
            repainter.install()
            repainter.uninstall()
            panel.repaints = 0
            repeat(3) { chart.fireChartChanged() }
            assertEquals(3, panel.repaints)
        }
    }
}
