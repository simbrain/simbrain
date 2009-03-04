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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;

/**
 * Open data world in current workspace.
 */
public final class GlobalUpdateAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;


    /**
     * Create an open data world with the specified
     * workspace.
     */
    public GlobalUpdateAction(Workspace workspace) {
        super("Global update", workspace);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
        putValue(SHORT_DESCRIPTION, "Global step network update algorithm");
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, toolkit.getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, keyStroke);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        workspace.singleUpdate();
    }
}