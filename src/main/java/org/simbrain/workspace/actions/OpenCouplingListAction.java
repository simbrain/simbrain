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
package org.simbrain.workspace.actions;

import org.simbrain.util.ResourceManager;
import org.simbrain.workspace.gui.CouplingListPanel;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Opens and displays a list of the current couplings.
 */
public final class OpenCouplingListAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to Simbrain desktop.
     */
    private SimbrainDesktop desktop;

    /**
     * Create a coupling list with the specified workspace.
     *
     * @param desktop reference to simbrain desktop.
     */
    public OpenCouplingListAction(final SimbrainDesktop desktop) {
        super("Open Coupling List...");
        this.desktop = desktop;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/CouplingList.png"));
        putValue(SHORT_DESCRIPTION, "Open coupling list");
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        final JFrame frame = new JFrame("Coupling List");
        CouplingListPanel cpl = new CouplingListPanel(desktop, desktop.getWorkspace().getCouplings());
        frame.setContentPane(cpl);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}