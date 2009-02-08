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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Save as network action.
 */
public final class SaveAsNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new save as network action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SaveAsNetworkAction(final NetworkPanel networkPanel) {

        super("Save As...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        putValue(SMALL_ICON, ResourceManager.getImageIcon("SaveAs.png"));

        this.networkPanel = networkPanel;
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        //networkPanel.getParentComponent().showSaveFileDialog();
    }
}