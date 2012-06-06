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

import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.PotentialAttribute;
import org.simbrain.workspace.Workspace;

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
    private final PotentialAttribute potentialProducer;

    /** Reference to consuming attribute. */
    private final PotentialAttribute potentialConsumer;

    /** The workspace this object belongs to. */
    private final Workspace workspace;

    /**
     * Creates a new instance.
     *
     * @param workspace The parent workspace.
     * @param description The description of the menu item.
     * @param producer The producer for the coupling.
     * @param consumer The consumer for the coupling.
     */
    @SuppressWarnings("unchecked")
    public CouplingMenuItem(final Workspace workspace,
            final String description, final PotentialAttribute producer,
            final PotentialAttribute consumer) {
        super(description, workspace.getCouplingManager()
                .containseEquivalentCoupling(
                        new Coupling(producer.createProducer(), consumer
                                .createConsumer())));
        this.workspace = workspace;
        this.potentialProducer = producer;
        this.potentialConsumer = consumer;

        addActionListener(listener);
    }

    /**
     * Listens for events where this item is clicked. If this item is selected
     * when there is no coupling one is created. If it is selected, then the
     * coupling is removed.
     */
    @SuppressWarnings("unchecked")
    private final ActionListener listener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            if (getState()) {
                workspace.addCoupling(new Coupling(potentialProducer
                        .createProducer(), potentialConsumer.createConsumer()));
                setSelected(true);
            } else {
                workspace.getCouplingManager().removeMatchingCoupling(
                        new Coupling(potentialProducer.createProducer(),
                                potentialConsumer.createConsumer()));
                setSelected(false);
            }
        }
    };

}
