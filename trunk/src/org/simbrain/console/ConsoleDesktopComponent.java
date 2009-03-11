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

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

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
        Interpreter interprerter = getSimbrainInterpreter(console, super
                .getWorkspaceComponent().getWorkspace());
        add("Center", console);
        new Thread(interprerter).start();
    }
    
    /**
     * Returns a Simbrain interpreter
     * 
     * @param console console for interpreter
     * @param workspace workaspace references
     * @return simbrain interpreter
     */
    public static Interpreter getSimbrainInterpreter(final JConsole console, final Workspace workspace) {
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
            String FS = System.getProperty("file.separator");
            interpreter.set("workspace", workspace);
            interpreter.set("bsh.prompt", ">");
            interpreter.eval("addClassPath(\"scripts" + FS + "console\");");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return interpreter;
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
