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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceSerializer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.WorkspaceChangedDialog;

/**
 * Import workspace.
 */
public final class ImportWorkspaceAction extends DesktopAction {

    private static final long serialVersionUID = 1L;
    
    private final WorkspaceSerializer serializer;
    
    private final Workspace workspace;
    
    /**
     * Create an import workspace action with the specified
     * workspace.
     */
    public ImportWorkspaceAction(SimbrainDesktop desktop) {
        super("Import Workspace", desktop);
        this.workspace = desktop.getWorkspace();
        serializer = new WorkspaceSerializer(workspace);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        if (workspace.changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(desktop);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }
        
        serializer.importWorkspace();
    }
}