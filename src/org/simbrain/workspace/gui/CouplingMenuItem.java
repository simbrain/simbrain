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

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;

import javax.swing.*;

/**
 * A menu item corresponding to a potential coupling. When the menuitem is
 * invoked, a coupling is created (See ActionPerformed in CouplingMenuItem.java).
 */
public class CouplingMenuItem extends JMenuItem {

    /**
     * The default serial version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Reference to producing attribute.
     */
    private Producer producer;

    /**
     * Reference to consuming attribute.
     */
    private Consumer consumer;

    /**
     * The workspace this object belongs to.
     */
    private Workspace workspace;

    /**
     * Creates a new instance.
     *
     * @param workspace   The parent workspace.
     * @param description The description of the menu item.
     * @param producer    The producer for the coupling.
     * @param consumer    The consumer for the coupling.
     */
    public CouplingMenuItem(Workspace workspace, String description, Producer producer, Consumer consumer) {
        super(description);
        this.setIcon(null);
        this.workspace = workspace;
        this.producer = producer;
        this.consumer = consumer;
        // Listen for events where this item is clicked.
        addActionListener(evt -> {
            Coupling coupling = workspace.getCouplingManager().tryCoupling(producer, consumer);
            setSelected(true);
        });
    }
}
