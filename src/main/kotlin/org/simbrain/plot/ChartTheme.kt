package org.simbrain.plot

import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.Axis
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.plot.DefaultDrawingSupplier
import org.jfree.chart.plot.PiePlot
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.category.BarRenderer
import org.jfree.chart.renderer.category.StandardBarPainter
import org.jfree.chart.renderer.xy.StandardXYBarPainter
import org.jfree.chart.renderer.xy.XYBarRenderer
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.chart.title.PaintScaleLegend
import org.simbrain.util.Theme
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Paint
import javax.swing.UIManager

/**
 * The cohesive categorical palette (Tableau 10) used for every chart's series. Reads cleanly on both
 * the light (white) and dark plot wells, unlike JFreeChart's saturated primary defaults. Exposed so
 * charts that assign their own series colors (the histogram, the bar chart) draw from the same set
 * rather than re-introducing the old red/blue/green/yellow.
 */
val CHART_SERIES_PALETTE: List<Color> = listOf(
    Color(0x4E, 0x79, 0xA7), Color(0xF2, 0x8E, 0x2B), Color(0xE1, 0x57, 0x59), Color(0x59, 0xA1, 0x4F),
    Color(0xED, 0xC9, 0x48), Color(0xB0, 0x7A, 0xA1), Color(0x76, 0xB7, 0xB2), Color(0xFF, 0x9D, 0xA7),
    Color(0x9C, 0x75, 0x5F), Color(0xBA, 0xB0, 0xAC)
)

/**
 * Palette entry [index] (wrapping around the palette) at the given [alpha] (0–255, default opaque).
 * The alpha overload backs the histogram, whose overlapping series blend through partial transparency.
 */
@JvmOverloads
fun chartSeriesColor(index: Int, alpha: Int = 255): Color {
    val base = CHART_SERIES_PALETTE[index.mod(CHART_SERIES_PALETTE.size)]
    return if (alpha >= 255) base else Color(base.red, base.green, base.blue, alpha)
}

private val SERIES_PALETTE: Array<Paint> = Array(CHART_SERIES_PALETTE.size) { CHART_SERIES_PALETTE[it] }

/** Line/series stroke, a touch heavier than JFreeChart's 1px default so lines read as deliberate. */
private val SERIES_STROKE = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

/** A fresh supplier per plot — [DefaultDrawingSupplier] is stateful (tracks the next-series index). */
private fun modernDrawingSupplier() = DefaultDrawingSupplier(
    SERIES_PALETTE,
    SERIES_PALETTE,
    DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
    arrayOf<java.awt.Stroke>(SERIES_STROKE),
    DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
    DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE
)

/**
 * Theme a [JFreeChart]'s chrome AND modernize how its data is drawn, from the active FlatLaf /
 * [Theme] tokens, so charts track light and dark instead of rendering as a fixed light card with
 * dark, near-invisible axis text under a dark theme.
 *
 * Chrome: chart/plot backgrounds, gridlines, axis lines, tick/label/title/legend text.
 * Data styling: flat bars (no gradient sheen, no drop shadow), heavier antialiased lines, flat pie
 * slices (no shadow/outline), and a cohesive [SERIES_PALETTE] for auto-assigned series.
 *
 * The palette is applied via the plot's drawing supplier, so any chart that sets explicit series
 * colors AFTER calling this (the histogram's per-series loop, the bar chart's model color, the
 * projection plot's custom renderer) still wins — those are domain colors owned by the model.
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
    // Colorbars are subtitles rather than plot axes, so themeAxis below never reaches their axis.
    subtitles.filterIsInstance<PaintScaleLegend>().forEach {
        it.backgroundPaint = chartBg
        themeAxis(it.axis, text, gridline)
    }
    when (val p = plot) {
        is XYPlot -> {
            p.backgroundPaint = plotBg
            p.outlinePaint = gridline
            p.domainGridlinePaint = gridline
            p.rangeGridlinePaint = gridline
            p.drawingSupplier = modernDrawingSupplier()
            themeAxis(p.domainAxis, text, gridline)
            themeAxis(p.rangeAxis, text, gridline)
            when (val r = p.renderer) {
                is XYBarRenderer -> {
                    r.barPainter = StandardXYBarPainter()
                    r.setShadowVisible(false)
                    r.setDrawBarOutline(false)
                }
                is XYLineAndShapeRenderer -> {
                    r.setAutoPopulateSeriesStroke(false)
                    r.defaultStroke = SERIES_STROKE
                }
            }
        }
        is CategoryPlot -> {
            p.backgroundPaint = plotBg
            p.outlinePaint = gridline
            p.domainGridlinePaint = gridline
            p.rangeGridlinePaint = gridline
            p.drawingSupplier = modernDrawingSupplier()
            themeAxis(p.domainAxis, text, gridline)
            themeAxis(p.rangeAxis, text, gridline)
            (p.renderer as? BarRenderer)?.let { r ->
                r.barPainter = StandardBarPainter()
                r.setShadowVisible(false)
                r.setDrawBarOutline(false)
            }
        }
        is PiePlot<*> -> {
            p.backgroundPaint = plotBg
            p.outlinePaint = gridline
            p.drawingSupplier = modernDrawingSupplier()
            p.shadowPaint = null
            p.setSectionOutlinesVisible(false)
            p.labelBackgroundPaint = chartBg
            p.labelPaint = text
            p.labelOutlinePaint = null
            p.labelShadowPaint = null
            p.labelLinkPaint = gridline
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
