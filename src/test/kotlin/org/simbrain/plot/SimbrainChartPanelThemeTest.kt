/**
 * A chart panel re-themes its own chart on a theme switch while it is on screen, and stops once it is taken off
 * screen, so no desktop-wide walk is needed to reach it.
 */
package org.simbrain.plot

import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.Theme
import org.simbrain.util.UiWork
import org.simbrain.util.onEdt
import java.awt.Color
import javax.swing.JFrame
import javax.swing.SwingUtilities

class SimbrainChartPanelThemeTest {

    private val stale = Color(1, 2, 3)

    private fun showChart(): Pair<JFrame, JFreeChart> {
        lateinit var frame: JFrame
        lateinit var chart: JFreeChart
        SwingUtilities.invokeAndWait {
            chart = ChartFactory.createXYLineChart("", "x", "y", null)
            frame = JFrame().apply {
                contentPane = SimbrainChartPanel(chart)
                pack()
            }
        }
        return frame to chart
    }

    @Test
    fun `a shown chart panel re-themes its chart on a theme switch`() {
        val (frame, chart) = showChart()
        try {
            SwingUtilities.invokeAndWait { chart.backgroundPaint = stale }

            Theme.changed.fireChanged()
            UiWork.awaitIdle()

            assertNotEquals(stale, onEdt { chart.backgroundPaint })
        } finally {
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }

    @Test
    fun `a chart panel taken off screen no longer re-themes`() {
        val (frame, chart) = showChart()
        SwingUtilities.invokeAndWait {
            frame.dispose()
            chart.backgroundPaint = stale
        }

        Theme.changed.fireChanged()
        UiWork.awaitIdle()

        assertEquals(stale, onEdt { chart.backgroundPaint })
    }
}
