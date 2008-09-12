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
package org.simbrain.workspace.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.WorkspaceComponentListPanel;

/**
 * Opens and displays a list of the current workspace components.
 */
public final class OpenWorkspaceComponentListAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Reference to Simbrain desktop. */
    private SimbrainDesktop desktop;
    

    /**
     * Create a workspace component list of the specified
     * workspace.
     *
     * @param desktop reference to simbrain desktop.
     */
    public OpenWorkspaceComponentListAction(final SimbrainDesktop desktop) {
        super("Open component list");
        this.desktop = desktop;
//        putValue(SMALL_ICON, ResourceManager.getImageIcon("BothWays.png"));
        putValue(SHORT_DESCRIPTION, "Open component list");
    }

    /**
     * @see AbstractAction
     * @param event Action event
     */
    public void actionPerformed(final ActionEvent event) {
        final JFrame frame = new JFrame("Coupling List");
        JComponent cl = new WorkspaceComponentListPanel(desktop);
        frame.setContentPane(cl);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}