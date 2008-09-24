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
package org.simbrain.plot.barchart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.gui.CouplingMenuItem;
import org.simbrain.workspace.gui.CouplingMenus;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;
import org.jfree.data.xy.XYSeries;

/**
 * Display a PieChart.
 */
public class BarChartGui extends GuiComponent<BarChartComponent> {

    /** The underlying plot component. */
    private final BarChartComponent component;
    
    /**
     * Construct the GUI Bar Chart.
     */
    public BarChartGui(final GenericFrame frame, final BarChartComponent component) {
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
        JFreeChart chart = ChartFactory.createBarChart(
                "Bar Chart Demo 1",       // chart title
                "Category",               // domain axis label
                "Value",                  // range axis label
                component.getDataset(),                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?

            );
        ChartPanel panel = new ChartPanel(chart);
        add("Center", panel);
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
    }
   
}
