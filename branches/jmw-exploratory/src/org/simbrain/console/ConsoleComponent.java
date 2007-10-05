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

import java.awt.Dimension;
import java.io.File;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

import bsh.Interpreter;
import bsh.util.JConsole;

/**
 * Component corresponding to a beanshell window.
 */
public class ConsoleComponent extends WorkspaceComponent {

    /**
     * Constructor.
     */
    public ConsoleComponent() {
        super();
        this.setPreferredSize(new Dimension(450,400));
        JConsole console = new JConsole();
        Interpreter interpreter = new Interpreter(console);
        interpreter.getNameSpace().importPackage("org.simnet.neurons");
        interpreter.getNameSpace().importPackage("org.simnet.connections");
        interpreter.getNameSpace().importPackage("org.simnet.layouts");
        interpreter.getNameSpace().importPackage("org.simnet.networks");
        interpreter.getNameSpace().importPackage("org.simnet.interfaces");
        interpreter.getNameSpace().importPackage("org.simnet.groups");
        interpreter.getNameSpace().importPackage("org.simnet.synapses");
        interpreter.getNameSpace().importPackage("org.simbrain.workspace");
        interpreter.getNameSpace().importCommands(".");
        interpreter.getNameSpace().importCommands(
                "org.simbrain.console.commands");
        interpreter.getOut();
        interpreter.getErr();
        try {
            interpreter.set("workspace", org.simbrain.workspace.Workspace.getInstance());
            interpreter.set("bsh.prompt", ">");
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setContentPane(console);
        new Thread(interpreter).start();
    }

    @Override
    public void close() {
    }

    @Override
    public String getFileExtension() {
        return null;
    }

    @Override
    public void open(File openFile) {
    }

    @Override
    public void save(File saveFile) {
    }

}
