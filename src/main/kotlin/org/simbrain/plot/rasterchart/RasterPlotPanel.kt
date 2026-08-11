/**
 * Swing panel for a [org.simbrain.plot.raster.RasterModel]: the JFreeChart scatter plot plus a
 * [ChartLegendPanel] legend strip and toolbar. The legend is Swing rather than JFreeChart's
 * in-chart title so each series can carry a hoverable remove control; series are deleted from
 * there, not from a toolbar button. Usable independently of the raster workspace component;
 * callers construct it and then call [init] to build the chart.
 */
package org.simbrain.plot.rasterchart

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.SymbolAxis
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.AbstractRenderer
import org.jfree.chart.renderer.xy.XYItemRenderer
import org.simbrain.plot.ChartLegendPanel
import org.simbrain.plot.applySimbrainChartTheme
import org.simbrain.plot.raster.RasterModel
import org.simbrain.util.Theme
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import javax.swing.JButton
import javax.swing.JPanel
import kotlin.math.max

class RasterPlotPanel(val rasterModel: RasterModel) : JPanel() {

    private lateinit var chart: JFreeChart

    val chartPanel: ChartPanel = ChartPanel(null)

    val buttonPanel: JPanel = JPanel()

    /**
     * Renderer object where things like dot color and size are set.
     */
    private lateinit var renderer: XYItemRenderer

    private val legendPanel = ChartLegendPanel()

    init {
        preferredSize = PREFERRED_SIZE
        layout = BorderLayout()

        addAddSeriesButton()
        addClearGraphDataButton()
        addPreferencesButton()

        add("Center", chartPanel)
        add("South", JPanel(BorderLayout()).apply {
            add(legendPanel, BorderLayout.NORTH)
            add(buttonPanel, BorderLayout.SOUTH)
        })
    }

    /**
     * Initialize Chart Panel.
     */
    fun init() {
        chart = ChartFactory.createScatterPlot(
            "",
            "Iterations",
            "Value(s)",
            rasterModel.dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )
        renderer = chart.xyPlot.renderer
        applyRowAxis()
        updateChartSettings()
        rasterModel.events.propertyChanged.on { updateChartSettings() }
        rasterModel.events.rasterConsumerAdded.on { rebuildLegend() }
        rasterModel.events.rasterConsumerRemoved.on { rebuildLegend() }
        chartPanel.chart = chart
        chart.applySimbrainChartTheme()
        rebuildLegend()
    }

    private fun rebuildLegend() {
        // Pin palette assignment to series order: lookupSeriesPaint hands out the next palette color
        // to whoever asks first, and swatches otherwise ask in reverse z-order during painting.
        (renderer as? AbstractRenderer)?.let { r ->
            rasterModel.rasterConsumerList.indices.forEach { r.lookupSeriesPaint(it) }
        }
        legendPanel.setEntries(rasterModel.rasterConsumerList.map { consumer ->
            ChartLegendPanel.Entry(
                label = rasterModel.dataset.getSeries(consumer.index).key.toString(),
                paint = { (renderer as? AbstractRenderer)?.lookupSeriesPaint(consumer.index) },
                onRemove = { rasterModel.removeDataSource(consumer) }
            )
        })
    }

    /**
     * Show the incoming array's component names down the row axis, e.g. neuron labels, falling back to the
     * row index for rows the producer did not name. The axis is rebuilt on every settings change, after
     * [applySimbrainChartTheme] has run, so it must theme itself or it comes up with dark-on-dark text and
     * opaque light grid bands under the dark theme.
     */
    private fun applyRowAxis() {
        val rows = max(max(rasterModel.rowCount, rasterModel.componentNames.size), 1)
        val labels = Array(rows) { row ->
            rasterModel.componentNames.getOrNull(row) ?: row.toString()
        }
        val axis = SymbolAxis("Value(s)", labels)
        axis.setRange(-0.5, rows - 0.5)
        axis.labelPaint = Theme.foreground
        axis.tickLabelPaint = Theme.foreground
        axis.axisLinePaint = Theme.divider
        axis.tickMarkPaint = Theme.divider
        axis.gridBandPaint = Theme.divider.let { Color(it.red, it.green, it.blue, 60) }
        axis.gridBandAlternatePaint = TRANSPARENT
        chart.xyPlot.rangeAxis = axis
    }

    fun updateChartSettings() {
        val size = rasterModel.dotSize.toDouble()
        val delta = size / 2.0
        val square = Rectangle2D.Double(-delta, -delta, size, size)
        val circle = Ellipse2D.Double(-delta, -delta, size, size)
        renderer.setSeriesShape(0, square)
        renderer.setSeriesShape(1, circle)
        renderer.setSeriesShape(2, square)
        renderer.setSeriesShape(3, circle)

        applyRowAxis()

        if (rasterModel.isFixedWidth) {
            chart.xyPlot.domainAxis.fixedAutoRange = rasterModel.windowSize.toDouble()
        } else {
            chart.xyPlot.domainAxis.fixedAutoRange = -1.0
            chart.xyPlot.domainAxis.isAutoRange = true
        }
    }

    /**
     * Remove all buttons from the button panel; used when customizing the buttons on this panel.
     */
    fun removeAllButtonsFromToolBar() {
        buttonPanel.removeAll()
    }

    fun addClearGraphDataButton() {
        val clearButton = JButton("Clear")
        clearButton.action = RasterPlotActions.getClearGraphAction(this)
        buttonPanel.add(clearButton)
    }

    fun addPreferencesButton() {
        val prefsButton = JButton("Prefs")
        prefsButton.hideActionText = true
        prefsButton.action = RasterPlotActions.getPropertiesDialogAction(this)
        buttonPanel.add(prefsButton)
    }

    fun addAddSeriesButton() {
        val addButton = JButton("Add")
        addButton.action = RasterPlotActions.getAddSourceAction(this)
        buttonPanel.add(addButton)
    }

    fun showPropertiesDialog() {
        rasterModel.createEditorDialog { updateChartSettings() }.display()
    }

    companion object {
        private val PREFERRED_SIZE = Dimension(500, 400)
        private val TRANSPARENT = Color(0, 0, 0, 0)
    }
}
