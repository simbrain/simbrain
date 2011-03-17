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
import java.io.File;

import javax.swing.AbstractAction;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceSerializer;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.dataworld.DataWorldPreferences;

/**
 * Open data world in current workspace.
 */
public final class OpenDataWorldAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;

    /**
     * Open a data world.
     *
     * @param workspace reference to workspace
     */
    public OpenDataWorldAction(Workspace workspace) {
        super("Data World", workspace);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        SFileChooser chooser = new SFileChooser(DataWorldPreferences
                .getCurrentDirectory(), "xml file", "xml");
        File theFile = chooser.showOpenDialog();
        if (theFile != null) {
            DataWorldComponent tableComponent = (DataWorldComponent) WorkspaceSerializer
                    .open(DataWorldComponent.class, theFile);
            workspace.addWorkspaceComponent(tableComponent);
        }
    }
}