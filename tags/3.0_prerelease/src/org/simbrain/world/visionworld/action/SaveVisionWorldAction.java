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
package org.simbrain.world.visionworld.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.visionworld.VisionWorldDesktopComponent;

/**
 * Create pixel matrix action.
 */
public final class SaveVisionWorldAction extends AbstractAction {

    /** Vision world. */
    private final VisionWorldDesktopComponent desktopComponent;

    /**
     * Create a new create pixel matrix action.
     *
     * @param visionWorld vision world, must not be null
     */
    public SaveVisionWorldAction(
            final VisionWorldDesktopComponent desktopComponent) {
        super("Save");
        if (desktopComponent == null) {
            throw new IllegalArgumentException(
                    "Desktop component must not be null");
        }
        this.desktopComponent = desktopComponent;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.png"));
        putValue(SHORT_DESCRIPTION, "Save Vision World");
    }

    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent event) {
        desktopComponent.save();
    }
}
