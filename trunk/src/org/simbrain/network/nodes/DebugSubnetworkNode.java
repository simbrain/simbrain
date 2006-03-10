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
package org.simbrain.network.nodes;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;

import org.simnet.interfaces.Network;

/**
 * Debug subnetwork node.
 */
public final class DebugSubnetworkNode
    extends SubnetworkNode2 {

    /**
     * Create a new debug subnetwork node.
     */
    public DebugSubnetworkNode(final NetworkPanel networkPanel,
                               final Network subnetwork,
                               final double x, final double y) {

        super(networkPanel, subnetwork, x, y);
    }


    /** @see ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement */
    protected String getToolTipText() {
        return getLabel();
    }

    /** @see ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new JMenu("Debug 0"));
        contextMenu.add(new JMenu("Debug 1"));
        return contextMenu;
    }

    /** @see ScreenElement */
    protected boolean hasPropertyDialog() {
        return false;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        return null;
    }
}