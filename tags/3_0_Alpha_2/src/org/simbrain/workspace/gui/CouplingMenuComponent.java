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

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * A menu for creating a set of couplings from one component to another. The
 * menu shows components. Selecting a menu items couples each visible attribute
 * in the source component to a visible attribute in the target component.
 */
public class CouplingMenuComponent extends JMenu implements WorkspaceListener {

    /** Reference to workspace. */
    private Workspace workspace;

    /** The component to couple to. */
    private WorkspaceComponent sourceComponent;

    /**
     * @param menuName the name of the menu
     * @param workspace the workspace
     * @param sourceComponent the source component
     */
    public CouplingMenuComponent(final String menuName, final Workspace workspace,
            WorkspaceComponent sourceComponent) {
        super(menuName);
        this.workspace = workspace;
        this.sourceComponent = sourceComponent;
        workspace.addListener(this);
        updateMenu();
    }

    /**
     * {@inheritDoc}
     */
	public boolean clearWorkspace() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
	public void componentAdded(WorkspaceComponent component) {
        updateMenu();
    }

    /**
     * {@inheritDoc}
     */
	public void componentRemoved(WorkspaceComponent component) {
        updateMenu();
    }

    /**
     * {@inheritDoc}
     */
    public void workspaceCleared() {
        updateMenu();
    }

    /**
     * {@inheritDoc}
     */
    public void newWorkspaceOpened() {
    }


	/**
     * Update the menu.
     */
    private void updateMenu() {
        this.removeAll();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            final WorkspaceComponent targetComponent = component;
            JMenuItem componentMenuItem = new JMenuItem(targetComponent
                    .getName());
            componentMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    workspace.coupleOneToOne(sourceComponent
                            .getPotentialProducers(), targetComponent
                            .getPotentialConsumers());
                }
            });
            this.add(componentMenuItem);
        }
    }

}