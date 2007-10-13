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

import org.simbrain.console.ConsoleComponent;
import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;

/**
 * Add console to workspace.
 */
public final class NewConsoleAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new console action with the specified
     * workspace.
     */
    public NewConsoleAction(Workspace workspace) {
        super("New Console", workspace);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Terminal2.png"));
        putValue(SHORT_DESCRIPTION, "New console");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        workspace.addWorkspaceComponent(new ConsoleComponent(workspace));
    }
}