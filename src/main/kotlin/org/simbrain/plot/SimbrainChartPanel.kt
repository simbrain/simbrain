/**
 * Simbrain's chart panel adds reliable modern-modifier panning to JFreeChart's Swing panel.
 *
 * JFreeChart's built-in panning looks for deprecated modifier masks, which do not consistently
 * recognize Command drags on current macOS runtimes. This subclass handles Command or Control
 * drags before JFreeChart can begin its rectangle-zoom gesture.
 */
package org.simbrain.plot

import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.Pannable
import org.jfree.chart.plot.Plot
import org.jfree.chart.plot.PlotOrientation
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent

class SimbrainChartPanel(chart: JFreeChart? = null) : ChartPanel(chart) {

    private var panLast: Point? = null
    private var panWidth = 0.0
    private var panHeight = 0.0

    override fun mousePressed(event: MouseEvent) {
        if (beginPan(event)) return
        super.mousePressed(event)
    }

    override fun mouseDragged(event: MouseEvent) {
        val previousPoint = panLast
        if (previousPoint == null) {
            super.mouseDragged(event)
            return
        }

        val horizontalDelta = event.x - previousPoint.x
        val verticalDelta = event.y - previousPoint.y
        if (horizontalDelta == 0 && verticalDelta == 0) return

        val pannable = chart?.plot as? Pannable ?: return
        val domainPercent = -horizontalDelta / panWidth
        val rangePercent = verticalDelta / panHeight
        val plot = pannable as Plot
        val notify = plot.isNotify
        plot.isNotify = false
        if (pannable.orientation == PlotOrientation.VERTICAL) {
            if (pannable.isDomainPannable) pannable.panDomainAxes(domainPercent, chartRenderingInfo.plotInfo, previousPoint)
            if (pannable.isRangePannable) pannable.panRangeAxes(rangePercent, chartRenderingInfo.plotInfo, previousPoint)
        } else {
            if (pannable.isDomainPannable) pannable.panDomainAxes(rangePercent, chartRenderingInfo.plotInfo, previousPoint)
            if (pannable.isRangePannable) pannable.panRangeAxes(domainPercent, chartRenderingInfo.plotInfo, previousPoint)
        }
        plot.isNotify = notify
        constrainSimbrainChartView()
        panLast = event.point
        event.consume()
    }

    override fun mouseReleased(event: MouseEvent) {
        if (panLast != null) {
            panLast = null
            cursor = Cursor.getDefaultCursor()
            event.consume()
            return
        }
        super.mouseReleased(event)
    }

    private fun beginPan(event: MouseEvent): Boolean {
        if (!event.isMetaDown && !event.isControlDown) return false
        val pannable = chart?.plot as? Pannable ?: return false
        if (!pannable.isDomainPannable && !pannable.isRangePannable) return false
        val dataArea = getScreenDataArea(event.x, event.y) ?: return false
        if (!dataArea.contains(event.point)) return false

        panWidth = dataArea.width
        panHeight = dataArea.height
        panLast = event.point
        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
        event.consume()
        return true
    }
}
