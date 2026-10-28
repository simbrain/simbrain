/**
 * Shared look of Simbrain's JFreeChart plots: the selectable series palettes and the theming pass that
 * styles a chart's chrome and data from the active light/dark tokens and [PlotPreferences].
 */
package org.simbrain.plot

import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.Axis
import org.jfree.chart.axis.SymbolAxis
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
 * Categorical palettes for chart series. Each reads cleanly on both the light (white) and dark plot
 * wells, unlike JFreeChart's saturated primary defaults, which is why none includes black or white.
 */
enum class ChartSeriesPalette(private val label: String, vararg rgb: Int) {

    TABLEAU_10(
        "Tableau 10",
        0x4E79A7, 0xF28E2B, 0xE15759, 0x59A14F, 0xEDC948, 0xB07AA1, 0x76B7B2, 0xFF9DA7, 0x9C755F, 0xBAB0AC
    ),

    /** Okabe-Ito, distinguishable under common color vision deficiencies; its black is swapped for gray. */
    OKABE_ITO(
        "Okabe-Ito (colorblind safe)",
        0xE69F00, 0x56B4E9, 0x009E73, 0xF0E442, 0x0072B2, 0xD55E00, 0xCC79A7, 0x999999
    ),

    DARK_2(
        "Dark 2",
        0x1B9E77, 0xD95F02, 0x7570B3, 0xE7298A, 0x66A61E, 0xE6AB02, 0xA6761D, 0x666666
    );

    val colors: List<Color> = rgb.map { Color(it) }

    /** The name followed by a swatch of each color, so a dropdown previews the palette. */
    override fun toString() = colors.joinToString("", "<html>$label&nbsp;&nbsp;", "</html>") {
        "<font size='+1' color='#%06X'>&#9632;</font>".format(it.rgb and 0xFFFFFF)
    }
}

/**
 * The categorical palette used for every chart's series, chosen in [PlotPreferences]. Exposed so
 * charts that assign their own series colors (the histogram, the bar chart) draw from the same set
 * rather than re-introducing the old red/blue/green/yellow.
 */
val CHART_SERIES_PALETTE: List<Color> get() = PlotPreferences.seriesPalette.colors

/**
 * Palette entry [index] (wrapping around the palette) at the given [alpha] (0–255, default opaque).
 * The alpha overload backs the histogram, whose overlapping series blend through partial transparency.
 */
@JvmOverloads
fun chartSeriesColor(index: Int, alpha: Int = 255): Color {
    val palette = CHART_SERIES_PALETTE
    val base = palette[index.mod(palette.size)]
    return if (alpha >= 255) base else Color(base.red, base.green, base.blue, alpha)
}

/**
 * Line/series stroke from [PlotPreferences.lineWidth], by default a touch heavier than JFreeChart's
 * 1px so lines read as deliberate.
 */
private fun seriesStroke() =
    BasicStroke(PlotPreferences.lineWidth.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

/** A fresh supplier per plot — [DefaultDrawingSupplier] is stateful (tracks the next-series index). */
private fun modernDrawingSupplier(): DefaultDrawingSupplier {
    val palette: Array<Paint> = CHART_SERIES_PALETTE.toTypedArray()
    return DefaultDrawingSupplier(
        palette,
        palette,
        DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
        arrayOf<java.awt.Stroke>(seriesStroke()),
        DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE
    )
}

/**
 * Theme a [JFreeChart]'s chrome AND modernize how its data is drawn, from the active FlatLaf /
 * [Theme] tokens, so charts track light and dark instead of rendering as a fixed light card with
 * dark, near-invisible axis text under a dark theme.
 *
 * Chrome: chart/plot backgrounds, gridlines, axis lines, tick/label/title/legend text.
 * Data styling: flat bars (no gradient sheen, no drop shadow), heavier antialiased lines, flat pie
 * slices (no shadow/outline), and the [CHART_SERIES_PALETTE] for auto-assigned series.
 *
 * The palette is applied via the plot's drawing supplier, so any chart that sets explicit series
 * colors AFTER calling this (the histogram's per-series loop, the bar chart's model color, the
 * projection plot's custom renderer) still wins — those are domain colors owned by the model. The
 * exception is a line/scatter renderer's series paints, which are reset and re-pinned in series order
 * on every call so a changed palette reaches open time series and raster plots.
 *
 * Call after the chart is built, and again to re-theme it (a light/dark switch or a [PlotPreferences]
 * change). Tokens resolve at call time, matching the rest of the app's resolve-on-build convention.
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
                    r.defaultStroke = seriesStroke()
                    // Drop paints cached from the previous supplier so a re-theme picks up a changed
                    // palette, then re-pin them in series order as the panels' legends expect
                    r.clearSeriesPaints(false)
                    p.dataset?.let { ds -> repeat(ds.seriesCount) { r.lookupSeriesPaint(it) } }
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

/**
 * Theme one axis from the active tokens. For an axis a panel rebuilds after [applySimbrainChartTheme]
 * has run (a [SymbolAxis] relabeled as rows come and go), which would otherwise come up with
 * JFreeChart's black text and opaque light grid bands under the dark theme.
 */
fun Axis.applySimbrainAxisTheme() = themeAxis(this, Theme.foreground, Theme.divider)

private fun themeAxis(axis: Axis?, text: Color, line: Color) {
    axis ?: return
    axis.labelPaint = text
    axis.tickLabelPaint = text
    axis.axisLinePaint = line
    axis.tickMarkPaint = line
    if (axis is SymbolAxis) {
        axis.gridBandPaint = Color(line.red, line.green, line.blue, 60)
        axis.gridBandAlternatePaint = TRANSPARENT
    }
}

private val TRANSPARENT = Color(0, 0, 0, 0)
