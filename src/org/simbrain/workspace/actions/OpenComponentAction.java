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
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.serialization.WorkspaceSerializer;

import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Open a workspace component of the generic type in the current workspace.
 */
public class OpenComponentAction<T extends WorkspaceComponent> extends WorkspaceAction {
    private static final long serialVersionUID = 1L;
    private final Class<T> type;

    /**
     * Create an open odor world action with the specified workspace.
     * @param workspace The workspace which will own the opened component.
     */
    public OpenComponentAction(Class<T> type, String name, String iconFile, Workspace workspace) {
        super(name, workspace);
        this.type = type;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon(iconFile));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String defaultDirectory = SimbrainPreferences.getString("workspace" + type.getSimpleName() + "Directory");
        SFileChooser chooser = new SFileChooser(defaultDirectory, "XML File", "xml");
        File file = chooser.showOpenDialog();
        if (file != null) {
            WorkspaceComponent component = WorkspaceSerializer.open(type, file);
            workspace.addWorkspaceComponent(component);
        }
    }
}