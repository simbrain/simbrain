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
package org.simbrain.console;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;

import bsh.Interpreter;
import bsh.util.JConsole;

/**
 * Component corresponding to a beanshell window.
 */
public class ConsoleDesktopComponent extends  GuiComponent<ConsoleComponent> {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public ConsoleDesktopComponent(GenericFrame frame, ConsoleComponent component) {
        super(frame, component);
        setPreferredSize(new Dimension(500, 400));
    }
    
    public void postAddInit() {
        setLayout(new BorderLayout());
        JConsole console = new JConsole();
        Interpreter interpreter = new Interpreter(console);
        interpreter.getNameSpace().importPackage("org.simbrain.network.neurons");
        interpreter.getNameSpace().importPackage("org.simbrain.network.connections");
        interpreter.getNameSpace().importPackage("org.simbrain.network.layouts");
        interpreter.getNameSpace().importPackage("org.simbrain.network.networks");
        interpreter.getNameSpace().importPackage("org.simbrain.network.interfaces");
        interpreter.getNameSpace().importPackage("org.simbrain.network.groups");
        interpreter.getNameSpace().importPackage("org.simbrain.network.synapses");
        interpreter.getNameSpace().importPackage("org.simbrain.workspace");
        interpreter.getNameSpace().importCommands(".");
        interpreter.getNameSpace().importCommands("org.simbrain.console.commands");
        interpreter.getOut();
        interpreter.getErr();
        try {
            interpreter.set("workspace", super.getWorkspaceComponent().getWorkspace());
            interpreter.set("bsh.prompt", ">");
        } catch (Exception e) {
            e.printStackTrace();
        }
        add("Center", console);
        new Thread(interpreter).start();
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
        
    }


    @Override
    protected void update() {
        // TODO Auto-generated method stub
        
    }
}
