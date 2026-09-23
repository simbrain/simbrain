/**
 * Consistent mouse and trackpad navigation for Simbrain's JFreeChart panels.
 *
 * Swing receives ordinary mouse-wheel and two-finger-scroll input as [MouseWheelEvent]s, which
 * JFreeChart can use to zoom around the pointer. Native pinch gestures are not part of Swing's
 * input API, so they cannot be handled portably here.
 */
package org.simbrain.plot

import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.ValueAxis
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.plot.ValueAxisPlot
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.plot.Zoomable
import org.jfree.data.Range
import org.simbrain.util.ResourceManager
import java.awt.Container
import java.awt.event.MouseWheelEvent
import javax.swing.JButton
import kotlin.math.max
import kotlin.math.pow

private const val NAVIGATION_INSTALLED_PROPERTY = "org.simbrain.plot.navigationInstalled"
private const val WHEEL_ZOOM_STEP = 1.1
internal const val MINIMUM_VISIBLE_DATA_PIXELS = 16.0

/** Add shared zoom-in, zoom-out, and reset-view controls to a chart toolbar or button panel. */
fun addChartNavigationControls(container: Container, chartPanel: ChartPanel) {
    if (chartPanel.chart?.plot !is Zoomable) return
    container.add(JButton(ResourceManager.getSmallIcon("menu_icons/ZoomIn.png")).apply {
        toolTipText = "Zoom in"
        addActionListener {
            chartPanel.zoomAtCenter { x, y -> chartPanel.zoomInBoth(x, y) }
            chartPanel.constrainSimbrainChartView()
        }
    })
    container.add(JButton(ResourceManager.getSmallIcon("menu_icons/ZoomOut.png")).apply {
        toolTipText = "Zoom out"
        addActionListener {
            chartPanel.zoomAtCenter { x, y -> chartPanel.zoomOutBoth(x, y) }
            chartPanel.constrainSimbrainChartView()
        }
    })
    container.add(JButton(ResourceManager.getSmallIcon("menu_icons/ZoomReset.png")).apply {
        toolTipText = "Reset view to automatic bounds"
        addActionListener { chartPanel.restoreAutoBounds() }
    })
}

private inline fun ChartPanel.zoomAtCenter(zoom: (Double, Double) -> Unit) {
    val dataArea = screenDataArea ?: return
    zoom(dataArea.centerX, dataArea.centerY)
}

/** Enable smooth pointer-anchored wheel zoom for every zoomable axis in this chart panel. */
fun ChartPanel.enableSimbrainChartNavigation() {
    if (getClientProperty(NAVIGATION_INSTALLED_PROPERTY) == true) return

    setMouseZoomable(true, false)
    setZoomAroundAnchor(true)
    enablePlotPanning()
    // JFreeChart's built-in handler only reads integer wheel ticks, which makes a trackpad feel
    // jumpy. This handler preserves fractional rotations from high-resolution wheels and trackpads.
    setMouseWheelEnabled(false)
    addMouseWheelListener { event -> zoomFromWheel(event) }
    putClientProperty(NAVIGATION_INSTALLED_PROPERTY, true)
}

/** ChartPanel maps Command-drag on macOS and Control-drag elsewhere to panning. */
private fun ChartPanel.enablePlotPanning() {
    when (val plot = chart?.plot) {
        is XYPlot -> {
            plot.isDomainPannable = true
            plot.isRangePannable = true
        }
        is CategoryPlot -> plot.isRangePannable = true
    }
}

private fun ChartPanel.zoomFromWheel(event: MouseWheelEvent) {
    val zoomable = chart?.plot as? Zoomable ?: return
    val rotation = event.preciseWheelRotation
    if (rotation == 0.0) return

    val point = translateScreenToJava2D(event.point)
    val plotInfo = chartRenderingInfo.plotInfo
    if (!plotInfo.dataArea.contains(point)) return

    val factor = WHEEL_ZOOM_STEP.pow(rotation)
    if (isDomainZoomable) zoomable.zoomDomainAxes(factor, plotInfo, point, true)
    if (isRangeZoomable) zoomable.zoomRangeAxes(factor, plotInfo, point, true)
    constrainSimbrainChartView()
    event.consume()
}

/** Keep manual views within the data and wide enough to show a visible slice of its initial extent. */
internal fun ChartPanel.constrainSimbrainChartView() {
    val plot = chart?.plot ?: return
    val dataArea = screenDataArea ?: return
    when (plot) {
        is XYPlot -> {
            repeat(plot.domainAxisCount) { index -> constrainAxis(plot, plot.getDomainAxis(index), dataArea.width) }
            repeat(plot.rangeAxisCount) { index -> constrainAxis(plot, plot.getRangeAxis(index), dataArea.height) }
        }
        is CategoryPlot -> repeat(plot.rangeAxisCount) { index -> constrainAxis(plot, plot.getRangeAxis(index), dataArea.height) }
    }
}

private fun constrainAxis(plot: ValueAxisPlot, axis: ValueAxis?, pixelSpan: Double) {
    axis ?: return
    val dataRange = plot.getDataRange(axis) ?: return
    val dataLength = dataRange.length
    if (!dataLength.isFinite() || dataLength <= 0.0 || pixelSpan <= 0.0) return

    val minimumLength = max(
        (dataLength * MINIMUM_VISIBLE_DATA_PIXELS / pixelSpan).coerceAtMost(dataLength),
        Math.ulp(dataRange.centralValue) * 2
    )
    val targetLength = axis.range.length.coerceIn(minimumLength, dataLength)
    val target = Range(axis.range.centralValue - targetLength / 2, axis.range.centralValue + targetLength / 2)
    axis.setRange(target.shiftInside(dataRange), false, true)
}

/** Shift a range without resizing it so that it remains completely inside [bounds]. */
private fun Range.shiftInside(bounds: Range): Range = when {
    lowerBound < bounds.lowerBound -> Range(bounds.lowerBound, bounds.lowerBound + length)
    upperBound > bounds.upperBound -> Range(bounds.upperBound - length, bounds.upperBound)
    else -> this
}
