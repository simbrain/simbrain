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

import javax.swing.JButton;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.updator.WorkspaceUpdatorListener;

/**
 * A JButton which is automatically disabled when the workspace is running.
 */
public final class SimbrainButton extends JButton {

    /**
     * Construct a button with a reference the workspace which, when running,
     * will disable the button.
     *
     * @param text jbutton text forwarded to jbutton constructor
     * @param workspace workspace reference
     */
    public SimbrainButton(String text, Workspace workspace) {

        super(text);

        // Listen to workspace updator so that this button can be enabled or
        // disabled depending on whether the workspace is running or not.
        workspace.getWorkspaceUpdator().addUpdatorListener(
                new WorkspaceUpdatorListener() {

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

}