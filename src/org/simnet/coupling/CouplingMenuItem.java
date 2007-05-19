/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simnet.coupling;

import javax.swing.JMenuItem;

/**
 * <b>CouplingMenuItem</b> allows a menu-item to carry a reference to an associated
 * coupling object.  This allows for communication between the various components of
 * simbrain via pop-up menus.
 */
public class CouplingMenuItem extends JMenuItem {

    /** Coupling for this menu item. */
    private Coupling coupling;


    /**
     * Create a new coupling menu item with the specified
     * menu text and coupling.
     *
     * @param menuText menu text for this menu item
     * @param coupling coupling for this menu item
     */
    public CouplingMenuItem(final String menuText, final Coupling coupling) {
        super(menuText);
        setCoupling(coupling);
    }


    /**
     * Return the coupling for this menu item.
     *
     * @return the coupling for this menu item
     */
    public Coupling getCoupling() {
        return coupling;
    }

    /**
     * Set the coupling for this menu item to <code>coupling</code>.
     *
     * @param coupling coupling for this menu item
     */
    public void setCoupling(final Coupling coupling) {
        this.coupling = coupling;
    }

}
