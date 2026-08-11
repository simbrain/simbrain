/**
 * Swing panel for a [TimeSeriesModel]: the JFreeChart line chart plus a custom legend strip and
 * toolbar. The legend is Swing rather than JFreeChart's in-chart title so each series can carry a
 * hoverable remove control; series are deleted from there, not from a toolbar button.
 */
package org.simbrain.plot.timeseries

import net.miginfocom.swing.MigLayout
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.ValueAxis.*
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.plot.ValueMarker
import org.jfree.chart.renderer.AbstractRenderer
import org.simbrain.plot.ChartLegendPanel
import org.simbrain.plot.applySimbrainChartTheme
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min


class TimeSeriesPlotPanel(val timeSeriesModel: TimeSeriesModel): JPanel() {

    private val chart: JFreeChart

    val chartPanel: ChartPanel = ChartPanel(null)

    val buttonPanel: JPanel = JPanel()

    private var addButton: JButton? = null

    /**
     * Swing replacement for JFreeChart's in-chart legend, so each entry can host a remove control.
     */
    private val legendPanel = ChartLegendPanel()

    /** The series keys the legend was last built from, to detect renames cheaply on redraw. */
    private var builtLegendKeys: List<String> = emptyList()

    /**
     * Whether the legend shows a remove control per series. Trainer panels turn this off because
     * their series are owned by the trainer, not the user.
     */
    var seriesRemovalEnabled = true
        set(value) {
            field = value
            rebuildLegend()
        }

    /**
     * Range markers tracked so their values participate in [updateChartSettings] auto-range
     * computation. Use [addRangeMarker] / [removeRangeMarker] instead of adding to the plot
     * directly so the axis range expands to keep markers visible.
     */
    private val rangeMarkers = mutableListOf<ValueMarker>()

    init {
        preferredSize = PREFERRED_SIZE
        layout = MigLayout("ins 0, gap 0px 0px")

        addClearGraphDataButton()
        addPreferencesButton()
        addAddSeriesButton()

        add(chartPanel, "wrap")
        add(legendPanel, "growx, wrap")
        add(buttonPanel)

        timeSeriesModel.events.propertyChanged.on { this.updateChartSettings() }
        timeSeriesModel.events.timeSeriesAdded.on { rebuildLegend() }
        timeSeriesModel.events.timeSeriesRemoved.on { rebuildLegend() }

        val title = ""
        val xLabel = "Time"
        val yLabel = "Value"
        chart = ChartFactory.createXYLineChart(
            title,
            xLabel,
            yLabel,
            timeSeriesModel.dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )
        chartPanel.chart = chart
        chart.applySimbrainChartTheme()

        updateChartSettings()
        rebuildLegend()

        chart.addProgressListener {
            updateChartSettings()
            // Renames happen in place without an add/remove event; catch them as the chart redraws
            if (builtLegendKeys != currentLegendKeys()) rebuildLegend()
        }
    }

    private fun currentLegendKeys() = timeSeriesModel.timeSeriesList.map { it.series.key.toString() }

    private fun rebuildLegend() {
        builtLegendKeys = currentLegendKeys()
        // Pin palette assignment to series order: lookupSeriesPaint hands out the next palette color
        // to whoever asks first, and swatches otherwise ask in reverse z-order during painting.
        (chart.xyPlot.renderer as? AbstractRenderer)?.let { renderer ->
            timeSeriesModel.timeSeriesList.indices.forEach { renderer.lookupSeriesPaint(it) }
        }
        legendPanel.setEntries(timeSeriesModel.timeSeriesList.mapIndexed { index, ts ->
            ChartLegendPanel.Entry(
                label = ts.series.key.toString(),
                paint = { (chart.xyPlot.renderer as? AbstractRenderer)?.lookupSeriesPaint(index) },
                onRemove = if (seriesRemovalEnabled) {
                    { timeSeriesModel.removeTimeSeries(ts) }
                } else null
            )
        })
    }

    fun updateChartSettings() {
        // No idea why this is needed, but it makes the width get updated upon closing the settings dialog

        timeSeriesModel.fixedWidth = timeSeriesModel.fixedWidth


        if (timeSeriesModel.isAutoRange) {

            // A series holding no data yet reports NaN bounds, as one just added for a restored neuron does.
            // Letting that reach the range would make the whole range NaN and the chart would draw nothing
            // until that one series received its first value.
            val dataMin = timeSeriesModel.timeSeriesList
                .mapNotNull { it.series.minY.takeUnless(Double::isNaN) }.minOrNull() ?: 0.0
            val dataMax = timeSeriesModel.timeSeriesList
                .mapNotNull { it.series.maxY.takeUnless(Double::isNaN) }.maxOrNull() ?: 0.0
            val markerMin = rangeMarkers.minOfOrNull { it.value } ?: dataMin
            val markerMax = rangeMarkers.maxOfOrNull { it.value } ?: dataMax
            val min = min(dataMin, markerMin)
            val max = max(dataMax, markerMax)

            val (lower, upper) = listOf(
                if (timeSeriesModel.useAutoRangeMaximumLowerBound) {
                    min(min, timeSeriesModel.autoRangeMaximumLowerBound)
                } else {
                    min
                },
                if (timeSeriesModel.useAutoRangeMinimumUpperBound) {
                    max(max, timeSeriesModel.autoRangeMinimumUpperBound)
                } else {
                    max
                }
            ).sorted()

            val delta = max(upper - lower, DEFAULT_AUTO_RANGE_MINIMUM_SIZE)

            // Give markers extra headroom on the side they occupy so their labels aren't clipped
            val upperMargin = if (rangeMarkers.any { it.value >= dataMax }) {
                DEFAULT_UPPER_MARGIN + MARKER_LABEL_MARGIN
            } else DEFAULT_UPPER_MARGIN
            val lowerMargin = if (rangeMarkers.any { it.value <= dataMin }) {
                DEFAULT_LOWER_MARGIN + MARKER_LABEL_MARGIN
            } else DEFAULT_LOWER_MARGIN

            setRangeIfChanged(lower - lowerMargin * delta, upper + upperMargin * delta)

        } else {
            chart.xyPlot.rangeAxis.isAutoRange = false
            setRangeIfChanged(timeSeriesModel.rangeLowerBound, timeSeriesModel.rangeUpperBound)
        }
    }

    /**
     * `rangeAxis.setRange` unconditionally emits an AxisChangeEvent → plot redraw → progress listener →
     * back here. Skip the call when bounds haven't moved so we don't feed that loop every frame.
     */
    private fun setRangeIfChanged(lower: Double, upper: Double) {
        val axis = chart.xyPlot.rangeAxis
        val current = axis.range
        if (current.lowerBound != lower || current.upperBound != upper) {
            axis.setRange(lower, upper)
        }
    }

    /**
     * Add a horizontal range marker whose value is included in the auto-range computation, so the
     * axis stays expanded enough to keep the marker visible.
     */
    fun addRangeMarker(marker: ValueMarker) {
        rangeMarkers.add(marker)
        chart.xyPlot.addRangeMarker(marker)
        updateChartSettings()
    }

    fun removeRangeMarker(marker: ValueMarker) {
        rangeMarkers.remove(marker)
        chart.xyPlot.removeRangeMarker(marker)
        updateChartSettings()
    }

    /**
     * Add a vertical domain marker. Domain markers do not participate in auto-range — the x-axis
     * is driven by [TimeSeriesModel.fixedWidth] / [TimeSeriesModel.windowSize] — so the marker can
     * sit far beyond the current data without squashing the visible data.
     */
    fun addDomainMarker(marker: ValueMarker) {
        chart.xyPlot.addDomainMarker(marker)
    }

    fun removeDomainMarker(marker: ValueMarker) {
        chart.xyPlot.removeDomainMarker(marker)
    }


    /**
     * Used when customzing buttons on this panel.
     */
    fun removeAllButtonsFromToolBar() {
        buttonPanel.removeAll()
    }

    fun addAddSeriesButton() {
        addButton = JButton("Add")
        addButton!!.action = TimeSeriesPlotActions.getAddSourceAction(this)
        buttonPanel.add(addButton)
    }

    fun addClearGraphDataButton() {
        val clearButton = JButton("Clear")
        clearButton.action = TimeSeriesPlotActions.getClearGraphAction(this)
        buttonPanel.add(clearButton)
    }


    fun addPreferencesButton() {
        val prefsButton = JButton("Prefs")
        prefsButton.hideActionText = true
        prefsButton.action = TimeSeriesPlotActions.getPropertiesDialogAction(this)
        buttonPanel.add(prefsButton)
    }

    fun showPropertiesDialog() {
        val dialog = timeSeriesModel.createEditorDialog { e: TimeSeriesModel? ->
            updateChartSettings()
        }
        dialog.display()
    }

    companion object {
        private val PREFERRED_SIZE = Dimension(500, 400)
        /** Extra fractional margin added to the auto-range when a marker sits at the data boundary,
         * so the marker's label has room to render without being clipped by the axis. */
        private const val MARKER_LABEL_MARGIN = 0.1
    }
}
