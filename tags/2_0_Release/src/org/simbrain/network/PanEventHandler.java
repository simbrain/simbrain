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
package org.simbrain.network;

import java.awt.event.InputEvent;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PPanEventHandler;

/**
 * Pan event handler.
 */
final class PanEventHandler
    extends PPanEventHandler {

    /**
     * Create a new pan event handler.
     */
    public PanEventHandler() {
        super();
        setEventFilter(new PanEventFilter());
    }


    /**
     * Pan event filter, accepts left mouse clicks, but only when the network
     * panel's edit mode is <code>EditMode.PAN</code>.
     */
    private class PanEventFilter
        extends PInputEventFilter {

        /**
         * Create a new pan event filter.
         */
        public PanEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }


        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            EditMode editMode = networkPanel.getEditMode();

            return (editMode.isPan() && super.acceptsEvent(event, type));
        }
    }
}