/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.odorworld.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.world.odorworld.OdorWorldPanel;

/**
 * Open plot action.
 */
public final class AddEntityAction extends AbstractAction {

	/**
	 * Reference to Panel; the action refers to the panel because it needs
	 * information on mouse clicks, etc.
	 */
    private final OdorWorldPanel worldPanel;

    /**
     * Create a new open plot action.
     *
     * @param component GUI component, must not be null.
     */
    public AddEntityAction(final OdorWorldPanel worldPanel) {
        super("Add Entity");
        this.worldPanel = worldPanel;
//        putValue(SMALL_ICON, ResourceManager.getImageIcon("PixelMatrix.png"));
//        putValue(SHORT_DESCRIPTION, "Create Pixel Matrix");
    }


    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent event) {
    	worldPanel.getWorld().addBasicEntity(new double[]{worldPanel.getSelectedPoint().x, worldPanel.getSelectedPoint().y});
    }
}
