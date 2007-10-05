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
import javax.swing.JFrame;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.couplingmanager.CouplingManager;

/**
 * Open data world in current workspace.
 */
public final class OpenCouplingManagerAction extends AbstractAction {

    /**
     * Create an open data world with the specified
     * workspace.
     */
    public OpenCouplingManagerAction() {
        super("Open coupling manager");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("BothWays.png"));
        putValue(SHORT_DESCRIPTION, "Open coupling manager");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        JFrame frame = new JFrame();
        CouplingManager cm = new CouplingManager(frame);
        frame.setContentPane(cm);
        frame.setSize(850, 420);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setResizable(true); // Maybe should allow this...
        frame.setVisible(true);
    }
}