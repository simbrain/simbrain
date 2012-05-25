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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

import org.simbrain.network.core.Network;
import org.simbrain.network.gui.NetworkPanel;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Creates a network script menu, which shows a menu item for each script in the
 * relevant directory.
 */
public class NetworkScriptMenu {

    /** Location of script menu directory. */
    private static final String SCRIPT_MENU_DIRECTORY = "scripts"
            + System.getProperty("file.separator") + "network";

    /**
     * Create script menu.
     *
     * @param network root network references
     * @return script JMenu
     */
    public static JMenu getNetworkScriptMenu(NetworkPanel panel) {
        JMenu scriptMenu = new JMenu("Scripts");
        scriptMenu.add(new RunScriptAction(panel));
        scriptMenu.addSeparator();
        for (Action action : getScriptActions(panel)) {
            scriptMenu.add(action);
        }
        return scriptMenu;
    }

    /**
     * Make a list of script actions by iterating through script menu directory.
     *
     * @return list of actions
     */
    private static List<Action> getScriptActions(NetworkPanel panel) {
        ArrayList<Action> list = new ArrayList<Action>();
        File dir = new File(SCRIPT_MENU_DIRECTORY);
        if (!dir.isDirectory()) {
            return null; // Throw exception instead?
        }
        // TODO: look for other endings and invoke relevant script types
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".bsh")) {
                list.add(new ScriptAction(file.getName(), panel));
            }
        }
        return list;
    }

    /**
     * Create an action based on the name of a script.
     */
    public static class ScriptAction extends AbstractAction {

        /** Name of script for use in actions (e.g. menu items). */
        private String scriptName;

        /** Reference to Root Network */
        private Network network;

        /** Reference to Network panel */
        private NetworkPanel networkPanel;


        /**
         * Construct a network script action. 
         * @param name name of the action.
         * @param panel reference to parent network panel
         */
        public ScriptAction(String name, NetworkPanel panel) {
            super(name);
            // putValue(SHORT_DESCRIPTION, name);
            this.scriptName = name;
            this.networkPanel = panel;
            this.network = panel.getNetwork();
        }

        /** @see AbstractAction */
        public void actionPerformed(final ActionEvent event) {

            Interpreter interpreter = new Interpreter();

            try {
                interpreter.set("network", network);
                interpreter.set("networkPanel", networkPanel);
                interpreter.source(SCRIPT_MENU_DIRECTORY
                        + System.getProperty("file.separator") + scriptName);
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
