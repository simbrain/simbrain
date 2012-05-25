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
package org.simbrain.network.desktop;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Run network script.
 */
public final class RunScriptAction extends AbstractAction {

    /** Reference to Network Panel*/
    private NetworkPanel networkPanel;

    /** Script directory. */
    private static final String SCRIPT_MENU_DIRECTORY = "."
        + System.getProperty("file.separator") + "scripts"  + System.getProperty("file.separator") + "network" ;


    /**
     * Create a new script action for the workspace.
     */
    public RunScriptAction(NetworkPanel networkPanel) {
        super("Run Script...");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Script.png"));
        this.networkPanel = networkPanel;
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        SFileChooser fileChooser = new SFileChooser(SCRIPT_MENU_DIRECTORY,
                "Run Script", "bsh");
        File scriptFile = fileChooser.showOpenDialog();
        if (scriptFile != null) {
            Interpreter interpreter = new Interpreter();

            try {
                interpreter.set("network", networkPanel.getNetwork());
                interpreter.set("networkPanel", networkPanel);
                interpreter.source(scriptFile.toString());
            } catch (FileNotFoundException e) {
               System.out.println("File not found");
               e.printStackTrace();
            } catch (IOException e) {
               System.out.println("IO Exception");
               e.printStackTrace();
            } catch (EvalError e) {
                System.out.println("Evaluation error");
                e.printStackTrace();
            }
        }
    }
}