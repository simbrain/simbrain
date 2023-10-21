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
package org.simbrain.plot.timeseries;

import kotlin.Unit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.simbrain.util.SwingUtilsKt;

import javax.swing.*;
import java.awt.*;

/**
 * Display a TimeSeriesPlot. This component can be used independently of the
 * time series workspace component.
 */
public class TimeSeriesPlotPanel extends JPanel {

    /**
     * Chart un-initialized instance.
     */
    private JFreeChart chart;

    /**
     * Initial size.
     */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /**
     * Panel for chart.
     */
    private ChartPanel chartPanel = new ChartPanel(null);

    /**
     * Data model.
     */
    private TimeSeriesModel model;

    /**
     * Button panel.
     */
    private JPanel buttonPanel = new JPanel();

    /**
     * Combo box to select coupling mode (array or scalar).
     */
    private JComboBox couplingModeComboBox;

    /**
     * Button to delete scalar time series.
     */
    private JButton deleteButton;

    /**
     * Button to add scalar time series
     */
    private JButton addButton;

    /**
     * Construct a time series panel.
     *
     * @param timeSeriesModel model underlying model
     */
    public TimeSeriesPlotPanel(TimeSeriesModel timeSeriesModel) {
        model = timeSeriesModel;
        setPreferredSize(PREFERRED_SIZE);
        setLayout(new BorderLayout());

        addClearGraphDataButton();
        addPreferencesButton();
        addAddDeleteButtons();

        add("Center", chartPanel);
        add("South", buttonPanel);

        model.getEvents().getPropertyChanged().on(this::updateChartSettings);

        init();

        updateChartSettings();
    }

    /**
     * Initialize Chart Panel.
     */
    public void init() {
        String title = "";
        String xLabel = "Time";
        String yLabel = "Value";
        boolean showLegend = true;
        boolean useTooltips = true;
        boolean generateUrls = false;
        chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, model.getDataset(), PlotOrientation.VERTICAL, true, true, false);
        chartPanel.setChart(chart);
        chart.setBackgroundPaint(null);

    }

    public void updateChartSettings() {

        // No idea why this is needed, but it makes the width get updated upon closing the settings dialog
        model.setFixedWidth(model.isFixedWidth());

        if (model.isAutoRange()) {
            if (model.isUseFixedRangeWindow()) {
                chart.getXYPlot().getRangeAxis().setAutoRange(false);
                chart.getXYPlot().getRangeAxis().setRange(model.getRangeLowerBound(), model.getFixedRangeThreshold());
            } else {
                chart.getXYPlot().getRangeAxis().setAutoRange(true);
            }
        } else {
            chart.getXYPlot().getRangeAxis().setAutoRange(false);
            chart.getXYPlot().getRangeAxis().setRange(model.getRangeLowerBound(), model.getRangeUpperBound());
        }
    }


    /**
     * Remove all buttons from the button panel; used when customzing the
     * buttons on this panel.
     */
    public void removeAllButtonsFromToolBar() {
        buttonPanel.removeAll();
    }

    /**
     * Return button panel in case user would like to add custom buttons.
     */
    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    /**
     * Add buttons for adding and deleting {@link TimeSeriesModel.ScalarTimeSeries} objects.
     */
    public void addAddDeleteButtons() {
        deleteButton = new JButton("Delete");
        deleteButton.setAction(TimeSeriesPlotActions.getRemoveSourceAction(this));
        addButton = new JButton("Add");
        addButton.setAction(TimeSeriesPlotActions.getAddSourceAction(this));
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);
    }

    /**
     * Add button for clearing graph data.
     */
    public void addClearGraphDataButton() {
        JButton clearButton = new JButton("Clear");
        clearButton.setAction(TimeSeriesPlotActions.getClearGraphAction(this));
        buttonPanel.add(clearButton);
    }

    /**
     * Add button for showing preferences.
     */
    public void addPreferencesButton() {
        JButton prefsButton = new JButton("Prefs");
        prefsButton.setHideActionText(true);
        prefsButton.setAction(TimeSeriesPlotActions.getPropertiesDialogAction(this));
        buttonPanel.add(prefsButton);
    }

    /**
     * Show properties dialog.
     */
    public void showPropertiesDialog() {
        var dialog = SwingUtilsKt.createEditorDialog(model, (e) -> {
            updateChartSettings();
            return Unit.INSTANCE;
        });
        SwingUtilsKt.display(dialog);
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public TimeSeriesModel getTimeSeriesModel() {
        return model;
    }
}
