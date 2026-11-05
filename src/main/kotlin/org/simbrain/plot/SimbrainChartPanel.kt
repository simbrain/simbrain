/**
 * Simbrain's chart panel adds reliable modern-modifier panning to JFreeChart's Swing panel, and re-themes its
 * chart on a [PlotPreferences] commit or a live theme switch.
 *
 * JFreeChart's built-in panning looks for deprecated modifier masks, which do not consistently
 * recognize Command drags on current macOS runtimes. This subclass handles Command or Control
 * drags before JFreeChart can begin its rectangle-zoom gesture.
 *
 * The re-theme subscription lives while the panel is displayable, so charts in desktop components, dialogs and
 * simulation frames all follow preferences without their owners wiring anything.
 */
package org.simbrain.plot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.Pannable
import org.jfree.chart.plot.Plot
import org.jfree.chart.plot.PlotOrientation
import org.simbrain.util.Theme
import org.simbrain.util.UiScope
import org.simbrain.util.onUi
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent

class SimbrainChartPanel(chart: JFreeChart? = null) : ChartPanel(chart) {

    private var panLast: Point? = null
    private var panWidth = 0.0
    private var panHeight = 0.0

    private var viewScope: CoroutineScope? = null

    override fun addNotify() {
        super.addNotify()
        viewScope?.cancel()
        viewScope = UiScope().also { scope ->
            PlotPreferences.onUi(scope) { retheme() }
            Theme.changed.onUi(scope) { retheme() }
        }
    }

    override fun removeNotify() {
        viewScope?.cancel()
        viewScope = null
        super.removeNotify()
    }

    /**
     * Re-reads the series palette, line width and theme colors. The parent is repainted too because legend
     * swatches beside the chart read the renderer's paints when they paint.
     */
    private fun retheme() {
        val chart = chart ?: return
        chart.applySimbrainChartTheme()
        chart.fireChartChanged()
        (parent ?: this).repaint()
    }

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
