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
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.swing.JButton
import javax.swing.JPanel
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

    private fun newChart(): JFreeChart = ChartFactory.createXYLineChart(
        "", "x", "y", XYSeriesCollection(XYSeries("series").apply {
            add(0.0, 0.0)
            add(10.0, 10.0)
        })
    )

    @Test
    fun `install enables smooth pointer anchored wheel zoom`() {
        SwingUtilities.invokeAndWait {
            val panel = CountingChartPanel(newChart())
            AdaptiveChartRepainter(panel).install()

            assertTrue(panel.isDomainZoomable)
            assertTrue(panel.isRangeZoomable)
            assertTrue(panel.zoomAroundAnchor)
            assertTrue(panel.chart.xyPlot.isDomainPannable)
            assertTrue(panel.chart.xyPlot.isRangePannable)

            panel.setSize(400, 300)
            panel.paint(BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB).graphics)
            val originalRange = panel.chart.xyPlot.domainAxis.range.length
            panel.dispatchEvent(MouseWheelEvent(
                panel, MouseWheelEvent.MOUSE_WHEEL, 0, 0, 200, 150, 200, 150, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1, -0.5
            ))
            assertTrue(panel.chart.xyPlot.domainAxis.range.length < originalRange)
        }
    }

    @Test
    fun `command drag pans without starting a zoom rectangle`() {
        SwingUtilities.invokeAndWait {
            val panel = SimbrainChartPanel(newChart())
            AdaptiveChartRepainter(panel).install()
            panel.setSize(400, 300)
            panel.paint(BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB).graphics)
            panel.zoomInBoth(200.0, 150.0)
            panel.constrainSimbrainChartView()
            val originalLowerBound = panel.chart.xyPlot.domainAxis.range.lowerBound
            val modifiers = InputEvent.META_DOWN_MASK or InputEvent.BUTTON1_DOWN_MASK

            panel.mousePressed(MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, modifiers, 200, 150, 1, false, MouseEvent.BUTTON1))
            panel.mouseDragged(MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, modifiers, 160, 150, 0, false, MouseEvent.NOBUTTON))
            panel.mouseReleased(MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, InputEvent.META_DOWN_MASK, 160, 150, 1, false, MouseEvent.BUTTON1))

            assertNotEquals(originalLowerBound, panel.chart.xyPlot.domainAxis.range.lowerBound)
        }
    }

    @Test
    fun `shared navigation controls zoom and reset the chart`() {
        SwingUtilities.invokeAndWait {
            val panel = SimbrainChartPanel(newChart())
            AdaptiveChartRepainter(panel).install()
            panel.setSize(400, 300)
            panel.paint(BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB).graphics)
            val originalRange = panel.chart.xyPlot.domainAxis.range.length
            val controls = JPanel()
            addChartNavigationControls(controls, panel)
            val buttons = controls.components.filterIsInstance<JButton>()

            assertEquals(3, buttons.size)
            buttons[0].doClick()
            assertTrue(panel.chart.xyPlot.domainAxis.range.length < originalRange)
            buttons[2].doClick()
            assertEquals(originalRange, panel.chart.xyPlot.domainAxis.range.length)
        }
    }

    @Test
    fun `manual navigation stays within useful data bounds`() {
        SwingUtilities.invokeAndWait {
            val panel = SimbrainChartPanel(newChart())
            AdaptiveChartRepainter(panel).install()
            panel.setSize(400, 300)
            panel.paint(BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB).graphics)
            val axis = panel.chart.xyPlot.domainAxis
            val dataRange = panel.chart.xyPlot.getDataRange(axis)
            val dataArea = panel.screenDataArea

            repeat(20) {
                panel.zoomOutBoth(dataArea.centerX, dataArea.centerY)
                panel.constrainSimbrainChartView()
            }
            assertEquals(dataRange.length, axis.range.length)

            repeat(20) {
                panel.zoomInBoth(dataArea.centerX, dataArea.centerY)
                panel.constrainSimbrainChartView()
            }
            val minimumLength = dataRange.length * MINIMUM_VISIBLE_DATA_PIXELS / dataArea.width
            assertTrue(axis.range.length + 1e-12 >= minimumLength)
        }
    }

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
