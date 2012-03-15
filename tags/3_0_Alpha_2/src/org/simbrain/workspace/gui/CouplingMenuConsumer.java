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

import javax.swing.JMenu;

import org.simbrain.workspace.PotentialAttribute;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Menu for making a single coupling. This menu is initialized with a potential
 * consumer. It then produces a hierarchy of menus: One menu for each component
 * in the workspace Within each component, a menuitem for each Potential
 * producer.
 */
public class CouplingMenuConsumer extends JMenu {

    /** Reference to workspace. */
    Workspace workspace;

    /** The base attribute for this menu. */
    PotentialAttribute consumer;

    /**
     * Construct the menu.
     *
     * @param menuName  the name of the menu
     * @param workspace  the workspace
     * @param consumer the target consuming attribute.
     */
    public CouplingMenuConsumer(final String menuName,
            final Workspace workspace, final PotentialAttribute consumer) {
        super(menuName);
        this.workspace = workspace;
        this.consumer = consumer;
        updateMenu();
    }

    /**
     * Update the menu to reflect current configuration of components in the
     * workspace.
     */
    private void updateMenu() {
        this.removeAll();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            JMenu componentMenu = new JMenu(component.getName());
            for (PotentialAttribute potentialProducer : component
                    .getPotentialProducers()) {
                CouplingMenuItem menuItem = new CouplingMenuItem(workspace,
                        potentialProducer.getDescription(), potentialProducer, consumer );
                componentMenu.add(menuItem);
            }

            this.add(componentMenu);
        }
    }


}