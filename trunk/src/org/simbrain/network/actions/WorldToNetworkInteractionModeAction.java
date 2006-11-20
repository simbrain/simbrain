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
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simnet.coupling.InteractionMode;

import org.simbrain.resource.ResourceManager;

/**
 * World to network interaction mode action.
 */
public final class WorldToNetworkInteractionModeAction
    extends InteractionModeAction {

    /**
     * Create a new world to network interaction mode action with the specified
     * network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public WorldToNetworkInteractionModeAction(final NetworkPanel networkPanel) {
        super("World to network", networkPanel, InteractionMode.WORLD_TO_NETWORK);

        // The image and description correspond to the last interaction mode this was in,
        //  so that the GUI representation shows the current mode, rather than the mode to go
        //  in to.
        //  TODO: Refactor this so it is more intuitive
        putValue(SMALL_ICON, ResourceManager.getImageIcon("BothWays.png"));
        putValue(SHORT_DESCRIPTION, "World and network are interacting");
    }
}