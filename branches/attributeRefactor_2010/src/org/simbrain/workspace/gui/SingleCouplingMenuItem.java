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

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;

/**
 * Packages an object with a jmenu item to make it easy to pass them along
 * through action events.
 *
 */
public class SingleCouplingMenuItem extends JCheckBoxMenuItem {

    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** Reference to producing attribute. */
    private final Producer<?> source;

    /** Reference to consuming attribute. */
    private final Consumer<?> target;

    /** The workspace this object belongs to. */
    private final Workspace workspace;

    /** The current coupling if there is one. */
    private final Coupling<?> coupling;

    /**
     * Creates a new instance.
     *
     * @param workspace The parent workspace.
     * @param description The description of the menu item.
     * @param source The producingAttribute for the coupling.
     * @param target The consumingAttribute for the coupling.
     */
    @SuppressWarnings("unchecked")
    public SingleCouplingMenuItem(final Workspace workspace, final String description,
            final Producer source,
            final Consumer target) {
        super(description,
                workspace.getCouplingManager().containsCoupling(new Coupling(source, target)));
        this.workspace = workspace;
        this.source = source;
        this.target = target;

        addActionListener(listener);

        coupling = new Coupling(source, target);
    }


    /**
     * Listens for events where this item is clicked.  If this item is selected when there
     * is no coupling one is created.  If it is selected, then the coupling is removed.
     */
    @SuppressWarnings("unchecked")
    private final ActionListener listener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            if (getState()) {
                workspace.addCoupling(coupling);
                setSelected(true);
            } else {
                workspace.removeCoupling(coupling);
                setSelected(false);
            }
        }
    };

    /**
     * @return the source
     */
    public Producer<?> getSource() {
        return source;
    }


    /**
     * @return the target
     */
    public Consumer<?> getTarget() {
        return target;
    }
}
