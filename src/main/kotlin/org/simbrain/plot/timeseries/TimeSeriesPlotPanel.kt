/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.plot.timeseries

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

/**
 * Display a TimeSeriesPlot. This component can be used independently of the
 * time series workspace component.
 */
class TimeSeriesPlotPanel(val timeSeriesModel: TimeSeriesModel): JPanel() {
    /**
     * Chart un-initialized instance.
     */
    private val chart: JFreeChart

    /**
     * Panel for chart.
     */
    val chartPanel: ChartPanel = ChartPanel(null)

    /**
     * Return button panel in case user would like to add custom buttons.
     */
    /**
     * Button panel.
     */
    val buttonPanel: JPanel = JPanel()

    /**
     * Combo box to select coupling mode (array or scalar).
     */
    private val couplingModeComboBox: JComboBox<*>? = null

    /**
     * Button to delete scalar time series.
     */
    private var deleteButton: JButton? = null

    /**
     * Button to add scalar time series
     */
    private var addButton: JButton? = null

    /**
     * Construct a time series panel.
     *
     * @param timeSeriesModel model underlying model
     */
    init {
        preferredSize = PREFERRED_SIZE
        layout = BorderLayout()

        addClearGraphDataButton()
        addPreferencesButton()
        addAddDeleteButtons()

        add("Center", chartPanel)
        add("South", buttonPanel)

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
     * Remove all buttons from the button panel; used when customzing the
     * buttons on this panel.
     */
    fun removeAllButtonsFromToolBar() {
        buttonPanel.removeAll()
    }

    /**
     * Add buttons for adding and deleting [TimeSeriesModel.TimeSeries] objects.
     */
    fun addAddDeleteButtons() {
        deleteButton = JButton("Delete")
        deleteButton!!.action = TimeSeriesPlotActions.getRemoveSourceAction(this)
        addButton = JButton("Add")
        addButton!!.action = TimeSeriesPlotActions.getAddSourceAction(this)
        buttonPanel.add(deleteButton)
        buttonPanel.add(addButton)
    }

    /**
     * Add button for clearing graph data.
     */
    fun addClearGraphDataButton() {
        val clearButton = JButton("Clear")
        clearButton.action = TimeSeriesPlotActions.getClearGraphAction(this)
        buttonPanel.add(clearButton)
    }

    /**
     * Add button for showing preferences.
     */
    fun addPreferencesButton() {
        val prefsButton = JButton("Prefs")
        prefsButton.hideActionText = true
        prefsButton.action = TimeSeriesPlotActions.getPropertiesDialogAction(this)
        buttonPanel.add(prefsButton)
    }

    /**
     * Show properties dialog.
     */
    fun showPropertiesDialog() {
        val dialog = timeSeriesModel.createEditorDialog { e: TimeSeriesModel? ->
            updateChartSettings()
            Unit
        }
        dialog.display()
    }

    companion object {
        /**
         * Initial size.
         */
        private val PREFERRED_SIZE = Dimension(500, 400)
    }
}
