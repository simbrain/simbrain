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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.gui.EditMode;
import org.simbrain.network.gui.NetworkPanel;

/**
 * Build mode action.
 */
class EditModeAction extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Build mode. */
    private final EditMode editMode;

    /**
     * Create a new edit mode action with the specified name, network panel, and
     * edit mode.
     *
     * @param name name
     * @param networkPanel network panel, must not be null
     * @param editMode edit mode, must not be null
     */
    EditModeAction(final String name, final NetworkPanel networkPanel,
            final EditMode editMode) {

        super(name);

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        if (editMode == null) {
            throw new IllegalArgumentException("editMode must not be null");
        }

        this.networkPanel = networkPanel;
        this.editMode = editMode;
    }

    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        networkPanel.setEditMode(editMode);
    }
}