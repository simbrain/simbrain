/**
 * Swing/JFreeChart view for the heat map.
 *
 * The colorbar is a chart *subtitle* rather than a plot axis, which is why [applySimbrainChartTheme]
 * had to learn about [PaintScaleLegend] and why it must run after the legend is added. The paint scale
 * is replaced rather than mutated on each refresh because its bounds are immutable and auto-range moves
 * them as data arrives.
 */
package org.simbrain.plot.heatmap

import net.miginfocom.swing.MigLayout
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.AxisLocation
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.axis.SymbolAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYBlockRenderer
import org.jfree.chart.title.PaintScaleLegend
import org.jfree.chart.ui.RectangleEdge
import org.jfree.chart.ui.RectangleInsets
import org.simbrain.plot.ChartColorMapPaintScale
import org.simbrain.plot.applySimbrainChartTheme
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.swingDispatcher
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel

class HeatMapPanel(val heatMapModel: HeatMapModel) : JPanel() {

    private val chart: JFreeChart

    private val plot: XYPlot

    private val renderer = XYBlockRenderer()

    private val colorBarAxis = NumberAxis("Value")

    val chartPanel: ChartPanel = ChartPanel(null)

    val buttonPanel: JPanel = JPanel()

    init {
        preferredSize = PREFERRED_SIZE
        layout = MigLayout("ins 0, gap 0px 0px")

        addClearButton()
        addPreferencesButton()

        add(chartPanel, "wrap")
        add(buttonPanel)

        val domainAxis = NumberAxis("Time").apply {
            standardTickUnits = NumberAxis.createIntegerTickUnits()
        }
        // Row 1 reads at the top, the orientation used for stacked-trial and population plots.
        val rangeAxis = createRowAxis()
        plot = XYPlot(heatMapModel.dataset(), domainAxis, rangeAxis, renderer)
        chart = JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false)
        chartPanel.chart = chart

        chart.addSubtitle(
            PaintScaleLegend(currentPaintScale(), colorBarAxis).apply {
                axisLocation = AxisLocation.BOTTOM_OR_RIGHT
                position = RectangleEdge.RIGHT
                margin = RectangleInsets(8.0, 4.0, 24.0, 4.0)
            }
        )

        chart.applySimbrainChartTheme()
        refresh()

        heatMapModel.events.propertyChanged.on(swingDispatcher) { refresh() }
    }

    /**
     * Rebuilds the dataset and color scale from the model. The paint scale is replaced rather than
     * mutated because its bounds are immutable and auto-range moves them as data arrives.
     */
    fun refresh() {
        val scale = currentPaintScale()
        renderer.paintScale = scale
        colorBarAxis.setRange(scale.lowerBound, scale.upperBound)
        chart.subtitles.filterIsInstance<PaintScaleLegend>().forEach { it.scale = scale }
        plot.dataset = heatMapModel.dataset()

        val columnWidth = columnSpacing()
        renderer.blockWidth = columnWidth
        // Blocks are anchored at their center, so each axis needs half a cell of padding to avoid
        // clipping the first and last row and column. Times are sorted rather than read off the ends
        // because workspace time can be reset mid-run, which would otherwise invert the range.
        val earliest = heatMapModel.times.minOrNull()?.toDouble() ?: 0.0
        val latest = heatMapModel.times.maxOrNull()?.toDouble() ?: 1.0
        plot.domainAxis.setRange(earliest - columnWidth / 2, latest + columnWidth / 2)
        plot.rangeAxis = createRowAxis().apply {
            setRange(-0.5, heatMapModel.rowCount.coerceAtLeast(1) - 0.5)
        }
    }

    /**
     * Width of one cell along the domain axis. The axis carries workspace time, which usually advances
     * one unit per column but need not — a plot fed every few iterations would otherwise draw
     * one-unit stripes separated by gaps — so the smallest observed gap is used as the cell width.
     */
    private fun columnSpacing(): Double {
        val gaps = heatMapModel.times.sorted().zipWithNext { earlier, later -> later - earlier }
            .filter { it > 0 }
        return gaps.minOrNull()?.toDouble() ?: 1.0
    }

    private fun currentPaintScale(): ChartColorMapPaintScale {
        val range = heatMapModel.colorRange()
        return ChartColorMapPaintScale(range.start, range.endInclusive) { heatMapModel.colorMap }
    }

    private fun createRowAxis() = SymbolAxis(
        "Row",
        Array(heatMapModel.rowCount.coerceAtLeast(1)) { row -> heatMapModel.rowLabels.getOrNull(row) ?: row.toString() }
    ).apply {
        isInverted = true
    }

    private fun addClearButton() {
        buttonPanel.add(JButton("Clear").apply { addActionListener { heatMapModel.clearData() } })
    }

    private fun addPreferencesButton() {
        buttonPanel.add(JButton("Prefs").apply { addActionListener { showPropertiesDialog() } })
    }

    fun showPropertiesDialog() {
        heatMapModel.createEditorDialog { refresh() }.display()
    }

    companion object {
        private val PREFERRED_SIZE = Dimension(500, 400)
    }
}
