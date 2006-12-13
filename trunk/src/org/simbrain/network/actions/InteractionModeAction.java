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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simnet.coupling.InteractionMode;

/**
 * Interaction mode action.
 */
class InteractionModeAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Interaction mode. */
    private final InteractionMode interactionMode;


    /**
     * Create a new interaction mode action with the specified
     * name, network panel, and interaction mode.
     *
     * @param name name
     * @param networkPanel network panel, must not be null
     * @param interactionMode interaction mode, must not be null
     */
    InteractionModeAction(final String name,
                          final NetworkPanel networkPanel,
                          final InteractionMode interactionMode) {

        super(name);

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        if (interactionMode == null) {
            throw new IllegalArgumentException("interactionMode must not be null");
        }

        this.networkPanel = networkPanel;
        this.interactionMode = interactionMode;
    }


    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        networkPanel.getRootNetwork().setInteractionMode(interactionMode);
    }
}