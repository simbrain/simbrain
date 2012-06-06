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
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.WorkspaceDialog;

/**
 * Opens a workspace property dialog.
 */
public final class ShowPropertyDialogAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Reference to Simbrain workspace. */
    private Workspace workspace;

    /**
     * Create a workspace component list of the specified workspace.
     *
     * @param desktop reference to simbrain desktop.
     */
    public ShowPropertyDialogAction(final Workspace workspace) {
        super("Workspace properties...");
        this.workspace = workspace;
        putValue(SHORT_DESCRIPTION, "Show workspace properties dialog.");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
    }

    /**
     * @see AbstractAction
     * @param event Action event
     */
    public void actionPerformed(final ActionEvent event) {
        WorkspaceDialog dialog = new WorkspaceDialog(workspace);
        dialog.setModal(true);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}