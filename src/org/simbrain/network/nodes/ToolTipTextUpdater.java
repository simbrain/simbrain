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

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import org.simbrain.network.NetworkPanel;

/**
 * Input event handler for a network node that updates tool tip text
 * for its NetworkPanel as the mouse enters and exits that node.
 *
 * <p>Usage:
 * <code>
 * final PNode node = ...;
 * node.addInputEventListener(new ToolTipTextUpdater() {
 *     protected String getToolTipText() {
 *       return node.toString();
 *     }
 *   });
 * </code>
 * </p>
 */
abstract class ToolTipTextUpdater
    extends PBasicInputEventHandler{

    /**
     * Return a string to use as tool tip text.
     *
     * @return a string to use as tool tip text
     */
    protected abstract String getToolTipText();


    /** @see PBasicInputEventHandler */
    public final void mouseEntered(final PInputEvent event) {

        event.setHandled(true);
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        //if (!networkPanel.isThreadRunning()) {
        networkPanel.setToolTipText(getToolTipText());
        //}
    }

    /** @see PBasicInputEventHandler */
    public final void mouseExited(final PInputEvent event) {

        event.setHandled(true);
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        //if (!networkPanel.isThreadRunning()) {
        networkPanel.setToolTipText(null);
        //}
    }
}