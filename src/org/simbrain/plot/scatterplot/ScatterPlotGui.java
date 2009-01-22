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
import java.awt.Color;
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
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.simbrain.plot.ChartListener;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.util.propertyeditor.TestObject;
import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.AttributeHolder;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a Scatter Plot.
 */
public class ScatterPlotGui extends GuiComponent<ScatterPlotComponent> implements ActionListener {

    /** Chart un-initialized instance. */
    private JFreeChart chart;

    /** Chart panel. */
    private ChartPanel chartPanel = new ChartPanel(null);

    /** Scatter plot component. */
    private ScatterPlotComponent component;

    /** XY chart renderer. */
    private XYDotRenderer renderer;

    /** Plot action manager. */
    private PlotActionManager actionManager;

    /** Preferred frame size. */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);
    
    /**
     * Construct the ScatterPlot.
     *
     * @param frame Generic frame for gui use
     * @param component Scatter plot component
     */
    public ScatterPlotGui(final GenericFrame frame, final ScatterPlotComponent component) {
        super(frame, component);
        this.component = component;
        setPreferredSize(new Dimension(PREFERRED_SIZE));
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());

        JButton deleteButton = new JButton("Delete");
        deleteButton.setActionCommand("Delete");
        deleteButton.addActionListener(this);
        JButton addButton = new JButton("Add");
        addButton.setActionCommand("Add");
        addButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);

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
        chart = ChartFactory.createScatterPlot("Scatter Plot Demo 1",
                "X", "Y", this.getWorkspaceComponent().getModel().getDataset(),
                PlotOrientation.VERTICAL, true, false, false);

        // Use below to make this stuff settable
        chart.getXYPlot().getDomainAxis().setRange(
                getWorkspaceComponent().getModel().getLowerDomainBoundary(),
                getWorkspaceComponent().getModel().getUpperDomainBoundary());
        chart.getXYPlot().getRangeAxis().setRange(
                getWorkspaceComponent().getModel().getLowerRangeBoundary(),
                getWorkspaceComponent().getModel().getUpperRangeBoundary());
        chart.getXYPlot().getDomainAxis().setAutoRange(
                getWorkspaceComponent().getModel().isAutoDomain());
        chart.getXYPlot().getRangeAxis().setAutoRange(
                getWorkspaceComponent().getModel().isAutoRange());
//        for (int i = 0; i < getWorkspaceComponent().getModel().getColor().size(); ++i) {
//            renderer.setSeriesPaint(i, getWorkspaceComponent().getModel().getColor().get(i));
//        }

        chartPanel.setChart(chart);
        renderer = new XYDotRenderer();
        chart.getXYPlot().setRenderer(renderer);

        getWorkspaceComponent().addListener(new ChartListener() {
            public void componentUpdated() {
            }

            public void setTitle(final String name) {
            }

            public void chartSettingsUpdated() {
                chart.getXYPlot().getDomainAxis().setAutoRange(
                        getWorkspaceComponent().getModel().isAutoDomain());
                if (!getWorkspaceComponent().getModel().isAutoDomain()) {
                    chart.getXYPlot().getDomainAxis().setRange(
                            getWorkspaceComponent().getModel().getLowerDomainBoundary(),
                            getWorkspaceComponent().getModel().getUpperDomainBoundary());
                }
                chart.getXYPlot().getRangeAxis().setAutoRange(
                        getWorkspaceComponent().getModel().isAutoRange());
                if (!getWorkspaceComponent().getModel().isAutoRange()) {
                    chart.getXYPlot().getRangeAxis().setRange(
                            getWorkspaceComponent().getModel().getLowerRangeBoundary(),
                            getWorkspaceComponent().getModel().getUpperRangeBoundary());
                }
                renderer.setDotHeight(getWorkspaceComponent().getModel().getDotSize());
                renderer.setDotWidth(getWorkspaceComponent().getModel().getDotSize());
                for (int i = 0; i < getWorkspaceComponent().getModel()
                        .getChartSeriesPaint().size(); ++i) {
                    renderer.setSeriesPaint(i, getWorkspaceComponent()
                            .getModel().getChartSeriesPaint().get(i));
                }
            }

            public void attributeRemoved(AttributeHolder holder,
                    Attribute attribute) {
                
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
            for (int i = getWorkspaceComponent().getModel().getChartSeriesPaint().size();
            i < chart.getXYPlot().getSeriesCount(); ++i) {
            getWorkspaceComponent().getModel().getChartSeriesPaint().add(
                    renderer.getSeriesPaint(i));
            }
            ScatterPlotDialog dialog = new ScatterPlotDialog(chart,
                    getWorkspaceComponent().getModel());
//            JDialog dialog = new JDialog();
//            dialog.setContentPane(new ReflectivePropertyEditor(getWorkspaceComponent()
//                  .getModel(), dialog));
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        } else if (arg0.getActionCommand().equalsIgnoreCase("Delete")) {
            this.getWorkspaceComponent().getModel().removeDataSource();
        } else if (arg0.getActionCommand().equalsIgnoreCase("Add")) {
            this.getWorkspaceComponent().getModel().addDataSource();
        }
    }
   
}
