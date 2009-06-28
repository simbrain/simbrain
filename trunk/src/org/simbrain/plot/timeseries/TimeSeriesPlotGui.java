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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.simbrain.plot.ChartSettingsListener;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a TimeSeriesPlot.
 */
public class TimeSeriesPlotGui extends GuiComponent<TimeSeriesPlotComponent>
            implements ActionListener {

    /** Chart un-initialized instance. */
    private JFreeChart chart;

    /** Plot action manager. */
    private PlotActionManager actionManager;

    /** Initial size. */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /** Panel for chart. */
    private ChartPanel chartPanel = new ChartPanel(null);

    /**
     * Construct a time series plot gui.
     * 
     * @param frame parent frame
     * @param component the underlying component
     */
    public TimeSeriesPlotGui(final GenericFrame frame, final TimeSeriesPlotComponent component) {
        super(frame, component);
        setPreferredSize(PREFERRED_SIZE);
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());

        JButton deleteButton = new JButton("Delete");
        deleteButton.setActionCommand("Delete");
        deleteButton.addActionListener(this);
        JButton addButton = new JButton("Add");
        addButton.setActionCommand("Add");
        addButton.addActionListener(this);
        JButton clearButton = new JButton("Clear");
        clearButton.setActionCommand("ClearData");
        clearButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);
        buttonPanel.add(clearButton);

        createAttachMenuBar();

        add("Center", chartPanel);
        add("South", buttonPanel);
    }

    /**
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
        
        // Generate the graph
        chart = ChartFactory.createXYLineChart(
            "Time series", // Title
            "Iterations", // x-axis Label
            "Value(s)", // y-axis Label
            this.getWorkspaceComponent().getModel().getDataset(), // Dataset
            PlotOrientation.VERTICAL, // Plot Orientation
            true, // Show Legend
            true, // Use tooltips
            false // Configure chart to generate URLs?
        );
        chartPanel.setChart(chart);

        // Sets the initial fixed width for the chart.
        chart.getXYPlot().getDomainAxis().setFixedAutoRange(getWorkspaceComponent().
                getModel().getWindowSize());

        chart.getXYPlot().getRangeAxis().setAutoRange(getWorkspaceComponent().
                getModel().isAutoRange());
        chart.getXYPlot().getDomainAxis().setAutoRange(getWorkspaceComponent().
                getModel().isAutoDomain());

        getWorkspaceComponent().setFixedWidth(getWorkspaceComponent().getModel().isFixedWindow());
        getWorkspaceComponent().setMaxSize(getWorkspaceComponent().getModel().getWindowSize());

        if (getWorkspaceComponent().getModel().isFixedWindow()) {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(getWorkspaceComponent().
                    getModel().getWindowSize());
        } else {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(-1);
        }
        

        chart.getXYPlot().getRangeAxis().setRange(getWorkspaceComponent().
                getModel().getLowerRangeBoundary(),
                getWorkspaceComponent().getModel().getUpperRangeBoundary());
        chart.getXYPlot().getDomainAxis().setRange(getWorkspaceComponent().
                getModel().getLowerDomainBoundary(),
                getWorkspaceComponent().getModel().getUpperDomainBoundary());

        // Notifies chart when changes are made within the dialog.
        getWorkspaceComponent().getModel().addChartSettingsListener(new ChartSettingsListener() {
            public void chartSettingsUpdated() {
                chart.getXYPlot().getDomainAxis().setFixedAutoRange(
                        getWorkspaceComponent().getModel().getWindowSize());

                chart.getXYPlot().getRangeAxis().setAutoRange(
                        getWorkspaceComponent().getModel().isAutoRange());
                chart.getXYPlot().getDomainAxis().setAutoRange(
                        getWorkspaceComponent().getModel().isAutoDomain());

                getWorkspaceComponent().setFixedWidth(
                        getWorkspaceComponent().getModel().isFixedWindow());
                if (getWorkspaceComponent().getModel().isFixedWindow()) {
                    getWorkspaceComponent().setMaxSize(
                            getWorkspaceComponent().getModel().getWindowSize());
                    chart.getXYPlot().getDomainAxis().setFixedAutoRange(
                            getWorkspaceComponent().getModel().getWindowSize());
                } else {
                    chart.getXYPlot().getDomainAxis().setFixedAutoRange(-1);
                }

                if (!getWorkspaceComponent().getModel().isAutoRange()) {
                    chart.getXYPlot().getRangeAxis().setRange(
                            getWorkspaceComponent().getModel().getLowerRangeBoundary(),
                            getWorkspaceComponent().getModel().getUpperRangeBoundary());
                }
                if (!getWorkspaceComponent().getModel().isAutoDomain()) {
                    chart.getXYPlot().getDomainAxis().setRange(
                            getWorkspaceComponent().getModel().getLowerDomainBoundary(),
                            getWorkspaceComponent().getModel().getUpperDomainBoundary());
                }
            }
        });
        getWorkspaceComponent().updateSettings();
    }

    /**
     * Creates the menu bar.
     */
    private void createAttachMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }

        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(this);
        preferences.setActionCommand("dialog");
        editMenu.add(preferences);

        bar.add(fileMenu);
        bar.add(editMenu);
        getParentFrame().setJMenuBar(bar);
    }

    @Override
    public void closing() {
    }

    @Override
    public void update() {
    }

    /** @see ActionListener */
    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("dialog")) {
            JDialog dialog = new JDialog();
            dialog.setContentPane(new ReflectivePropertyEditor(getWorkspaceComponent()
                    .getModel(), dialog));
            dialog.setModal(true);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            getWorkspaceComponent().getModel().update();
        } else if (arg0.getActionCommand().equalsIgnoreCase("Delete")) {
            this.getWorkspaceComponent().getModel().removeDataSource();
        } else if (arg0.getActionCommand().equalsIgnoreCase("Add")) {
            this.getWorkspaceComponent().getModel().addDataSource();
        } else if (arg0.getActionCommand().equalsIgnoreCase("ClearData")) {
            this.getWorkspaceComponent().getModel().clearData();
        }
    }
   
}
