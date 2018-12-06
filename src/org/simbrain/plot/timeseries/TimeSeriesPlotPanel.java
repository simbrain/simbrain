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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.simbrain.util.propertyeditor.gui.ReflectivePropertyEditor;

import javax.swing.*;
import java.awt.*;

/**
 * Display a TimeSeriesPlot. This component can be used independently of the
 * time series workspace component.
 * <p>
 * TODO: Make a version that extends this, like network panel case. Then
 * document in UML
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

        add("Center", chartPanel);
        add("South", buttonPanel);

        init();
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

        // // Create chart settings listener
        // model.addChartSettingsListener(new ChartSettingsListener() {
        //     public void chartSettingsUpdated(ChartModel theModel) {
        //         chart.getXYPlot().getRangeAxis().setAutoRange(model.isAutoRange());
        //         if (!model.isAutoRange()) {
        //             chart.getXYPlot().getRangeAxis().setRange(model.getRangeLowerBound(), model.getRangeUpperBound());
        //         }
        //         chart.getXYPlot().getDomainAxis().setAutoRange(true);
        //         //chart.getXYPlot().getDomainAxis().setFixedAutoRange(model.getMaximumDataPoints());
        //     }
        // });
        //
        // // Invoke an initial event in order to set default settings
        // model.fireSettingsChanged();
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
     *
     * @return
     */
    public JPanel getButtonPanel() {
        return buttonPanel;
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
        ReflectivePropertyEditor editor = (new ReflectivePropertyEditor(model));
        JDialog dialog = editor.getDialog();
        dialog.setModal(true);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * @return the chartPanel
     */
    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    /**
     * @return the model
     */
    public TimeSeriesModel getTimeSeriesModel() {
        return model;
    }
}
