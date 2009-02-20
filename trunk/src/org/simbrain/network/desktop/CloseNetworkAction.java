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
package org.simbrain.network.desktop;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.gui.NetworkPanel;

/**
 * Close network action.
 */
public final class CloseNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkDesktopComponent networkDesktopComponent;


    /**
     * Create a new close network action with the specified
     * network panel.
     *
     * @param networkDesktopComponent networkPanel, must not be null
     */
    public CloseNetworkAction(final NetworkDesktopComponent networkDesktopComponent) {

        super("Close");

        if (networkDesktopComponent == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkDesktopComponent = networkDesktopComponent;

        this.putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkDesktopComponent.getParentFrame().dispose();
    }
}