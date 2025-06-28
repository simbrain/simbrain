package org.simbrain.plot.timeseries

import net.miginfocom.swing.MigLayout
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.ValueAxis.*
import org.jfree.chart.plot.PlotOrientation
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min


class TimeSeriesPlotPanel(val timeSeriesModel: TimeSeriesModel): JPanel() {

    private val chart: JFreeChart

    val chartPanel: ChartPanel = ChartPanel(null)

    val buttonPanel: JPanel = JPanel()

    private var deleteButton: JButton? = null

    private var addButton: JButton? = null

    init {
        preferredSize = PREFERRED_SIZE
        layout = MigLayout("ins 0, gap 0px 0px")

        addClearGraphDataButton()
        addPreferencesButton()
        addAddDeleteButtons()

        add(chartPanel, "wrap")
        add(buttonPanel)

        timeSeriesModel.events.propertyChanged.on { this.updateChartSettings() }

        val title = ""
        val xLabel = "Time"
        val yLabel = "Value"
        val showLegend = true
        val useTooltips = true
        val generateUrls = false
        chart = ChartFactory.createXYLineChart(
            title,
            xLabel,
            yLabel,
            timeSeriesModel.dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        )
        chartPanel.chart = chart
        chart.backgroundPaint = null

        updateChartSettings()

        chart.addProgressListener {
            updateChartSettings()
        }
    }

    fun updateChartSettings() {
        // No idea why this is needed, but it makes the width get updated upon closing the settings dialog

        timeSeriesModel.fixedWidth = timeSeriesModel.fixedWidth


        if (timeSeriesModel.isAutoRange) {

            val min = timeSeriesModel.timeSeriesList.minOfOrNull { it.series.minY } ?: 0.0
            val max = timeSeriesModel.timeSeriesList.maxOfOrNull { it.series.maxY } ?: 0.0

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

            chart.xyPlot.rangeAxis.setRange(lower - DEFAULT_LOWER_MARGIN * delta, upper + DEFAULT_UPPER_MARGIN * delta)

        } else {
            chart.xyPlot.rangeAxis.isAutoRange = false
            chart.xyPlot.rangeAxis.setRange(timeSeriesModel.rangeLowerBound, timeSeriesModel.rangeUpperBound)
        }
    }


    /**
     * Used when customzing buttons on this panel.
     */
    fun removeAllButtonsFromToolBar() {
        buttonPanel.removeAll()
    }

    fun addAddDeleteButtons() {
        deleteButton = JButton("Delete")
        deleteButton!!.action = TimeSeriesPlotActions.getRemoveSourceAction(this)
        addButton = JButton("Add")
        addButton!!.action = TimeSeriesPlotActions.getAddSourceAction(this)
        buttonPanel.add(deleteButton)
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
    }
}
