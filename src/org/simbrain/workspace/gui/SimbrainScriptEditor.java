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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.ScriptEditor;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJInternalFrame;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Extension of script editor configured to be able to run and edit Simbrain
 * scripts.
 *
 * @author jeffyoshimi
 */
public class SimbrainScriptEditor extends ScriptEditor {

    /** Reference to desktop. */
    private final SimbrainDesktop desktop;

    /**
     * Initialize the script editor panel with some initial text.
     *
     * @param initialText the initial text.
     */
    public SimbrainScriptEditor(SimbrainDesktop desktop,
            final String initialText) {
        super(initialText);
        this.desktop = desktop;

    }

    /**
     * Returns the action for clearing the desktop of everything but this
     * component
     *
     * TODO: Not yet implemented!
     *
     * @param frame parent frame
     * @param editor reference to frame
     * @return action for clearing all
     */
    private static Action getClearAllButThisAction(final GenericFrame frame,
            final SimbrainScriptEditor editor) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear all");
                putValue(Action.NAME, "Clear all");
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                //editor.getDesktop().clearDesktop(frame);
            }


        };
    }

    /**
     * Returns the action for running script files.
     *
     * @param frame parent frame
     * @param editor reference to editor
     * @return action for running script file
     */
    private static Action getRunScriptAction(final GenericFrame frame,
            final SimbrainScriptEditor editor) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
                putValue(SHORT_DESCRIPTION, "Run Script");
                putValue(Action.NAME, "Run Script");
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                Interpreter interpreter = new Interpreter();
                try {
                    interpreter.set("desktop", editor.getDesktop());
                    interpreter.set("workspace", editor.getDesktop()
                            .getWorkspace());
                    interpreter.eval(editor.getText());
                } catch (EvalError e) {
                    System.err.println("Evaluation error");
                    e.printStackTrace();
                }
            }


        };
    }

    /**
     * @return the desktop
     */
    public SimbrainDesktop getDesktop() {
        return desktop;
    }

    /**
     * Returns an internal frame for editing a script.
     *
     * @param desktop reference to desktop
     * @param scriptFile file containing initial editor material
     * @return the internal frame with the embedded simbrain script editor
     */
    public static GenericJInternalFrame getInternalFrame(
            final SimbrainDesktop desktop, final File scriptFile) {
        GenericJInternalFrame frame = new GenericJInternalFrame();
        SimbrainScriptEditor editor = new SimbrainScriptEditor(desktop,
                Utils.readFileContents(scriptFile));
        editor.setScriptFile(scriptFile);
        editor.initFrame(frame, editor);
        return frame;
    }

    /**
     * Initialize the frame with the provided panel.
     *
     * @param frame frame to initialize
     * @param editor the panel to dispaly in the frame
     */
    private void initFrame(final GenericFrame frame,
            final SimbrainScriptEditor editor) {
        final JPanel mainPanel = new JPanel(new BorderLayout());
        createAttachMenuBar(frame, editor);
        JToolBar toolbar = editor.getToolbarOpenClose(frame, editor);
        toolbar.addSeparator();
        toolbar.add(getRunScriptAction(frame, editor));
        mainPanel.add("North", toolbar);
        mainPanel.add("Center", editor);
        frame.setContentPane(mainPanel);
        if (editor.getScriptFile() != null) {
            frame.setTitle(editor.getScriptFile().getName());
        }
    }

}
