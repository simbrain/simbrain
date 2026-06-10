package org.simbrain.plot

import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.Axis
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.plot.PiePlot
import org.jfree.chart.plot.XYPlot
import org.simbrain.util.Theme
import java.awt.Color
import javax.swing.UIManager

/**
 * Theme a [JFreeChart]'s chrome from the active FlatLaf / [Theme] tokens so charts track light and
 * dark instead of rendering as a fixed light card with dark, near-invisible axis text under a dark
 * theme. Only chrome is touched — backgrounds, gridlines, axis lines, tick/label text, title, and
 * legend. Series colors (bars, lines, pie slices, projection points) are domain data owned by each
 * plot's model and are deliberately left alone.
 *
 * Call once after the chart is built. Tokens resolve at call time, matching the rest of the app's
 * resolve-on-build convention.
 */
fun JFreeChart.applySimbrainChartTheme() {
    val chartBg = UIManager.getColor("Panel.background") ?: Theme.cardBg
    val plotBg = UIManager.getColor("Table.background") ?: Color.WHITE
    val gridline = Theme.divider
    val text = Theme.foreground

    backgroundPaint = chartBg
    title?.paint = text
    legend?.let {
        it.backgroundPaint = chartBg
        it.itemPaint = text
    }
    when (val p = plot) {
        is XYPlot -> {
            p.backgroundPaint = plotBg
            p.outlinePaint = gridline
            p.domainGridlinePaint = gridline
            p.rangeGridlinePaint = gridline
            themeAxis(p.domainAxis, text, gridline)
            themeAxis(p.rangeAxis, text, gridline)
        }
        is CategoryPlot -> {
            p.backgroundPaint = plotBg
            p.outlinePaint = gridline
            p.domainGridlinePaint = gridline
            p.rangeGridlinePaint = gridline
            themeAxis(p.domainAxis, text, gridline)
            themeAxis(p.rangeAxis, text, gridline)
        }
        is PiePlot<*> -> {
            p.backgroundPaint = plotBg
            p.outlinePaint = gridline
            p.labelBackgroundPaint = chartBg
            p.labelPaint = text
        }
    }
}

private fun themeAxis(axis: Axis?, text: Color, line: Color) {
    axis ?: return
    axis.labelPaint = text
    axis.tickLabelPaint = text
    axis.axisLinePaint = line
    axis.tickMarkPaint = line
}
