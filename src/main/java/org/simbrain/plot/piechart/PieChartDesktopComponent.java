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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Display a PieChart.
 */
public class PieChartDesktopComponent extends DesktopComponent<PieChartComponent> implements ActionListener {

    /**
     * Plot action manager.
     */
    private PlotActionManager actionManager;

    /**
     * Chart Panel.
     */
    private ChartPanel chartPanel = new ChartPanel(null);

    /**
     * Preferred frame size.
     */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /**
     * Chart gui.
     */
    private JFreeChart chart;

    /**
     * Construct the GUI Pie Chart.
     *
     * @param frame     Generic Frame
     * @param component Pie chart component
     */
    public PieChartDesktopComponent(final GenericFrame frame, final PieChartComponent component) {
        super(frame, component);
        setPreferredSize(PREFERRED_SIZE);
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());

        createAttachMenuBar();

        add("Center", chartPanel);

        chart = ChartFactory.createPieChart("", getWorkspaceComponent().getModel().getDataset(), true, true, false);
        chartPanel.setChart(chart);
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
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(this));

        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(this);
        preferences.setActionCommand("dialog");
        editMenu.add(preferences);

        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Plot/pie_chart.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        parentFrame.setJMenuBar(bar);
    }

    public void actionPerformed(ActionEvent e) {
        // As of now, there are no properties to set
        // if (e.getActionCommand().equalsIgnoreCase("dialog")) {
        //     AnnotatedPropertyEditor editor = (new AnnotatedPropertyEditor(getWorkspaceComponent().getModel()));
        //     JDialog dialog = editor.getDialog();
        //     dialog.setModal(true);
        //     dialog.pack();
        //     dialog.setLocationRelativeTo(null);
        //     dialog.setVisible(true);
        // }
    }
}
