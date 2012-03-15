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
package org.simbrain.network.gui.nodes;

import org.simbrain.network.gui.NetworkPanel;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

/**
 * Input event handler for a network node that updates tool tip text for its
 * NetworkPanel as the mouse enters and exits that node.
 * <p>
 * Usage: <code>
 * final PNode node = ...;
 * node.addInputEventListener(new ToolTipTextUpdater() {
 *     protected String getToolTipText() {
 *       return node.toString();
 *     }
 *   });
 * </code>
 * </p>
 */
abstract class ToolTipTextUpdater extends PBasicInputEventHandler {

    /** Network Panel. */
    private final NetworkPanel networkPanel;

    /**
     * Construct a new tool tip text updater with a reference to a network
     * panel.
     *
     * @param networkPanel reference to network panel.
     */
    public ToolTipTextUpdater(NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
    }

    /**
     * Return a string to use as tool tip text.
     *
     * @return a string to use as tool tip text
     */
    protected abstract String getToolTipText();

    /** @see PBasicInputEventHandler */
    public final void mouseEntered(final PInputEvent event) {

        event.setHandled(true);
        // if (!networkPanel.isThreadRunning()) {
        networkPanel.getCanvas().setToolTipText(getToolTipText());
        // }
    }

    /** @see PBasicInputEventHandler */
    public final void mouseExited(final PInputEvent event) {

        event.setHandled(true);
        // if (!networkPanel.isThreadRunning()) {
        networkPanel.getCanvas().setToolTipText(null);
        // }
    }
}