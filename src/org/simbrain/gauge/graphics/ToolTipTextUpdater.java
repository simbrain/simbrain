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
package org.simbrain.gauge.graphics;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

/**
 * Input event handler for a gauge datapoint that updates tool tip text
 * for its GaugePanel as the mouse enters and exits that node.
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
     * Get the high dimensional coordinates of a point.
     *
     * @return the high dimensional coordinates of a point.
     */
    protected abstract String getUpstairsText();

    /**
     * Get the low dimensional coordinates of a point.
     *
     * @return the low dimensional coordinates.
     */
    protected abstract String getDownstairsText();

    /** @see PBasicInputEventHandler */
    public final void mouseEntered(final PInputEvent event) {

        GaugePanel gaugePanel = (GaugePanel) event.getComponent();
        if (event.isAltDown()) {
            gaugePanel.setToolTipText(getDownstairsText());
        } else {
            gaugePanel.setToolTipText(getUpstairsText());
        }
    }

    /** @see PBasicInputEventHandler */
    public final void mouseExited(final PInputEvent event) {
        GaugePanel networkPanel = (GaugePanel) event.getComponent();
        networkPanel.setToolTipText(null);
    }
}