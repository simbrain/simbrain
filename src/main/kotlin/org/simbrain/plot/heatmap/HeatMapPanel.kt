package org.simbrain.plot.heatmap

import net.miginfocom.swing.MigLayout
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.AxisLocation
import org.jfree.chart.axis.NumberAxis
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
        val rangeAxis = NumberAxis("Row").apply {
            standardTickUnits = NumberAxis.createIntegerTickUnits()
            isInverted = true
        }
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
        // Blocks are anchored at their center, so the axes need half a cell of padding to avoid
        // clipping the first and last row and column.
        plot.domainAxis.setRange(
            (heatMapModel.times.firstOrNull()?.toDouble() ?: 0.0) - 0.5,
            (heatMapModel.times.lastOrNull()?.toDouble() ?: 1.0) + 0.5
        )
        plot.rangeAxis.setRange(-0.5, heatMapModel.rowCount.coerceAtLeast(1) - 0.5)
    }

    private fun currentPaintScale(): ChartColorMapPaintScale {
        val range = heatMapModel.colorRange()
        return ChartColorMapPaintScale(range.start, range.endInclusive) { heatMapModel.colorMap }
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
