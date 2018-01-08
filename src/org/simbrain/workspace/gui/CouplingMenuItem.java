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
package org.simbrain.workspace.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import org.simbrain.workspace.*;

/**
 * A menu item corresponding to a potential coupling. When the menuitem is
 * invoked, a coupling is created (see ActionPerformed in CouplingMenuItem.java)
 * It's a checkbox menu item. Checking it creates the coupling, unchecking it
 * removes it.
 */
public class CouplingMenuItem extends JCheckBoxMenuItem {

    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** Reference to producing attribute. */
    private Producer producer;

    /** Reference to consuming attribute. */
    private Consumer consumer;

    /** The workspace this object belongs to. */
    private Workspace workspace;

    /**
     * Creates a new instance.
     *
     * @param workspace The parent workspace.
     * @param description The description of the menu item.
     * @param producer The producer for the coupling.
     * @param consumer The consumer for the coupling.
     */
    public CouplingMenuItem(Workspace workspace, String description, Producer producer, Consumer consumer) {
        super(description);
        this.workspace = workspace;
        this.producer = producer;
        this.consumer = consumer;
        addActionListener(listener);
    }

    /**
     * Listens for events where this item is clicked. If this item is selected
     * when there is no coupling one is created. If it is selected, then the
     * coupling is removed.
     */
    private final ActionListener listener = new ActionListener() {
        public void actionPerformed(final ActionEvent evt) {
            if (getState()) {
                Coupling coupling = workspace.getCouplingFactory().tryCoupling(producer, consumer);
                setSelected(true);
            } else {
                setSelected(false);
            }
        }
    };

}
