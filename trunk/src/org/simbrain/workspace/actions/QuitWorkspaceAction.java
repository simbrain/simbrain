/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceChangedDialog;

/**
 * Clear the current workspace.
 */
public final class QuitWorkspaceAction
    extends AbstractAction {

    /** Workspace. */
    private final Workspace workspace;


    /**
     * Create a clear workspace action with the specified
     * workspace.
     *
     * @param workspace workspace, must not be null
     */
    public QuitWorkspaceAction(final Workspace workspace) {

        super("Quit");

        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }

        this.workspace = workspace;

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Q, toolkit.getMenuShortcutKeyMask());

        putValue(ACCELERATOR_KEY, keyStroke);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        if (workspace.changesExist()) {
            WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(workspace);
            if (!dialog.hasUserCancelled()) {
                workspace.disposeAllFrames();
                System.exit(0);
            } else {
                return;
            }
        }
        workspace.disposeAllFrames();
        System.exit(0);
    }
}