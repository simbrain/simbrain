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
package org.simbrain.plot.piechart;

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
import org.simbrain.plot.ChartSettingsListener;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a PieChart.
 */
public class PieChartGui extends GuiComponent<PieChartComponent> implements ActionListener {

    /** Plot action manager. */
    private PlotActionManager actionManager;

    /** Chart Panel. */
    private ChartPanel chartPanel = new ChartPanel(null);

    /** Preferred frame size. */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /** Chart gui. */
    private JFreeChart chart;
    
    /**
     * Construct the GUI Pie Chart.
     *
     * @param frame Generic Frame
     * @param component Pie chart component
     */
    public PieChartGui(final GenericFrame frame, final PieChartComponent component) {
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
        chart = ChartFactory.createPieChart(
            "Pie Chart",
            this.getWorkspaceComponent().getModel().getDataset(),
            true, // include legend
            true,
            false
        );
        chartPanel.setChart(chart);

        getWorkspaceComponent().getModel().addChartSettingsListener(new ChartSettingsListener() {
            public void chartSettingsUpdated() {
                chart.getPlot().setOutlineVisible(getWorkspaceComponent()
                        .getModel().isOutlineVisible());
            }
        });
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
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equalsIgnoreCase("Add")) {
            this.getWorkspaceComponent().getModel().addDataSource();
        } else if (e.getActionCommand().equalsIgnoreCase("dialog")) {
            ReflectivePropertyEditor editor = (new ReflectivePropertyEditor(
                    getWorkspaceComponent().getModel()));
            JDialog dialog = editor.getDialog();
            dialog.setModal(true);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        } else if (e.getActionCommand().equalsIgnoreCase("Delete")) {
            this.getWorkspaceComponent().getModel().removeDataSource();
        }
        
    }
   
}
