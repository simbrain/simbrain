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

import javax.swing.KeyStroke;

import org.simbrain.network.gui.EditMode;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Zoom in edit mode action.
 */
public final class ZoomEditModeAction
    extends EditModeAction {

    /**
     * Create a new zoom in edit mode action with the specified network
     * panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomEditModeAction(final NetworkPanel networkPanel) {
        super("Zoom in", networkPanel, EditMode.ZOOM_IN);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("ZoomIn.png"));
        putValue(SHORT_DESCRIPTION, "Zoom mode (z)");

    }

}