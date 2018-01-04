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
import javax.swing.JOptionPane;

import org.simbrain.workspace.MismatchedAttributesException;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * A menu for creating a set of couplings from one component to another. The
 * menu shows components. Selecting a menu items couples each visible attribute
 * in the source component to a visible attribute in the target component.
 *
 * NOTE: This method is no longer being used. The basic idea is that it just
 * goes through all possible couplings between two components and makes them
 * all, but there are many complications. In particular it's not at all obvious
 * to the user what will happen when this is used. Vector couplings handle most
 * of what is was built for. However in case a use is found for this later it
 * has not been deleted yet. (JY 10/13)
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
    public CouplingMenuComponent(final String menuName,
            final Workspace workspace, WorkspaceComponent sourceComponent) {
        super(menuName);
        this.workspace = workspace;
        this.sourceComponent = sourceComponent;
        workspace.addListener(this);
        updateMenu();
    }

    /**
     * {@inheritDoc}
     * @return
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
            JMenuItem componentMenuItem = new JMenuItem(
                    targetComponent.getName());
            componentMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        workspace.coupleOneToOne(sourceComponent.getProducers(),
                                targetComponent.getConsumers());
                    } catch (MismatchedAttributesException e1) {
                        JOptionPane.showMessageDialog(null, e1.getMessage(),
                                "Unmatched Attributes",
                                JOptionPane.WARNING_MESSAGE, null);
                    }
                }
            });
            this.add(componentMenuItem);
        }
    }

}