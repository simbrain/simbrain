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

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.updater.WorkspaceUpdaterListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Action to call workspace iterate once.
 */
public final class WorkspaceIterateAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;

    /**
     * Create an open data world with the specified workspace.
     *
     * @param workspace The workspace to update.
     */
    public WorkspaceIterateAction(Workspace workspace) {
        super("Iterate", workspace);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
        putValue(SHORT_DESCRIPTION, "Iterate Workspace Once");
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, toolkit.getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, keyStroke);

        // Listen to workspace updater so that this button can be enabled or
        // disabled depending on whether the workspace is running or not.
        workspace.getUpdater().addUpdaterListener(new WorkspaceUpdaterListener() {
            public void changeNumThreads() {
            }

            public void changedUpdateController() {
            }

            public void updatingStarted() {
                setEnabled(false);
            }

            public void updatingFinished() {
                setEnabled(true);
            }

            public void updatedCouplings(int update) {
            }

            public void workspaceUpdated() {
            }
        });
    }

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(ActionEvent event) {
        workspace.iterate();
    }
}