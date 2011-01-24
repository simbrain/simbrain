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
package org.simbrain.workspace.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.EditorPanel;
import org.simbrain.workspace.gui.GenericJInternalFrame;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Add a script editor the current workspace.
 */
public final class OpenEditorAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Reference to Simbrain desktop. */
    private SimbrainDesktop desktop;

    /**
     * Create an editor.
     */
    public OpenEditorAction(final SimbrainDesktop desktop) {
        super("Script editor");
        this.desktop = desktop;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Script.png"));
        putValue(SHORT_DESCRIPTION, "Create a script editor");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        GenericJInternalFrame frame = new GenericJInternalFrame();
        desktop.addInternalFrame(frame);
        EditorPanel panel = new EditorPanel();
        frame.setTitle("Script Editor");
        frame.setContentPane(panel);
        frame.setResizable(true);
        frame.setClosable(true);
        frame.setMaximizable(true);
        frame.setIconifiable(true);
        frame.setVisible(true);
        frame.pack();
    }
}