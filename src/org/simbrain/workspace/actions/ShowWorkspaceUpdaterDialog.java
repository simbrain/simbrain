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
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.WorkspaceUpdateManagerPanel;

/**
 * Show workspace updater panel.
 */
public final class ShowWorkspaceUpdaterDialog extends AbstractAction {

    /** Network panel. */
    private final SimbrainDesktop desktop;

    /**
     * Construct the show dialog action.
     *
     * @param desktop parent desktop
     */
    public ShowWorkspaceUpdaterDialog(final SimbrainDesktop desktop) {

        super("Edit Update Sequence...");
        if (desktop == null) {
            throw new IllegalArgumentException("desktop must not be null");
        }
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Sequence.png"));

        this.desktop = desktop;
    }

    /** @see AbstractAction 
     * @param event
     */
    public void actionPerformed(final ActionEvent event) {
        StandardDialog dialog = new StandardDialog();
        final WorkspaceUpdateManagerPanel updatePanel = new WorkspaceUpdateManagerPanel(
                desktop.getWorkspace(), dialog);
        dialog.setContentPane(updatePanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}