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
package org.simbrain.plot.scatterplot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a Scatter Plot.
 */
public class ScatterPlotGui extends GuiComponent<ScatterPlotComponent> implements ActionListener {

    /** The underlying plot component. */
    private final ScatterPlotComponent component;

    /** Chart un-initialized instance. */
    private JFreeChart chart;

    /** XY chart renderer. */
    private XYDotRenderer renderer;

    /** Plot action manager. */
    private PlotActionManager actionManager;
    
    /**
     * Construct the ScatterPlot.
     *
     * @param frame Generic frame for gui use
     * @param component Scatter plot component
     */
    public ScatterPlotGui(final GenericFrame frame, final ScatterPlotComponent component) {
        super(frame, component);
        this.component = component;
        setPreferredSize(new Dimension(500, 400));
        actionManager = new PlotActionManager(this);
    }

    /**
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
        setLayout(new BorderLayout());
        
        // Generate the graph
        chart = ChartFactory.createScatterPlot("Scatter Plot Demo 1",
                "X", "Y", component.getDataset(), PlotOrientation.VERTICAL, true, false, false);

        renderer = new XYDotRenderer();
        chart.getXYPlot().setRenderer(renderer);
        

        // Use below to make this stuff settable
        chart.getXYPlot().getDomainAxis().setRange(0, 100);
        chart.getXYPlot().getRangeAxis().setRange(0, 100);
        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        chart.getXYPlot().getRangeAxis().setAutoRange(false);


        JButton deleteButton = new JButton("Delete");
        deleteButton.setActionCommand("Delete");
        deleteButton.addActionListener(this);
        JButton addButton = new JButton("Add");
        addButton.setActionCommand("Add");
        addButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);
        
        ChartPanel panel = new ChartPanel(chart);

        createAttachMenuBar();

        add("Center", panel);
        add("South", buttonPanel);
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
            ScatterPlotDialog dialog = new ScatterPlotDialog(chart, renderer, component);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        } else if (arg0.getActionCommand().equalsIgnoreCase("Delete")) {
            component.removeDataSource();
        } else if (arg0.getActionCommand().equalsIgnoreCase("Add")) {
            component.addDataSource();
        }
    }
   
}
