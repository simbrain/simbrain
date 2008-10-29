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

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a TimeSeriesPlot.
 */
public class TimeSeriesPlotGui extends GuiComponent<TimeSeriesPlotComponent> implements ActionListener {

    /** The underlying plot component. */
    private final TimeSeriesPlotComponent component;

    /** Chart un-initialized instance. */
    private JFreeChart chart;

    /**
     * Construct a time series plot gui.
     * 
     * @param frame parent frame
     * @param component the underlying component
     */
    public TimeSeriesPlotGui(final GenericFrame frame, final TimeSeriesPlotComponent component) {
        super(frame, component);
        this.component = component;
        setPreferredSize(new Dimension(500, 400));
    }

    /**
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
        setLayout(new BorderLayout());
        
        // Generate the graph
        chart = ChartFactory.createXYLineChart(
            "Time series", // Title
            "Iterations", // x-axis Label
            "Value(s)", // y-axis Label
            component.getDataset(), // Dataset
            PlotOrientation.VERTICAL, // Plot Orientation
            true, // Show Legend
            true, // Use tooltips
            false // Configure chart to generate URLs?
        );

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
        
        ChartPanel panel = new ChartPanel(chart);
        
        add("North", createMenuBar());
        add("Center", panel);
        add("South", buttonPanel);

        // Sets the initial fixed width for the chart.
        chart.getXYPlot().getDomainAxis().setFixedAutoRange(component.getMaxSize());
    }

    /**
     * Creates the menu bar.
     * @return menu bar
     */
    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(this);
        preferences.setActionCommand("dialog");
        editMenu.add(preferences);
        bar.add(editMenu);
        return bar;
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
            TimeSeriesPlotDialog dialog = new TimeSeriesPlotDialog(chart, component);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        } else if (arg0.getActionCommand().equalsIgnoreCase("Delete")) {
            component.removeDataSource();
        } else if (arg0.getActionCommand().equalsIgnoreCase("Add")) {
            component.addDataSource();
        } else if (arg0.getActionCommand().equalsIgnoreCase("ClearData")) {
            component.clearData();
        }
    }
   
}
