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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericJInternalFrame;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager;

/**
 * Open data world in current workspace.
 */
public final class OpenCouplingManagerAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Reference to Simbrain desktop. */
    private SimbrainDesktop desktop;

    /**
     * Create an open data world with the specified
     * workspace.
     */
    public OpenCouplingManagerAction(final SimbrainDesktop desktop) {
        super("Open coupling manager");
        this.desktop = desktop;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Coupling.png"));
        putValue(SHORT_DESCRIPTION, "Open coupling manager");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        GenericJInternalFrame frame = new GenericJInternalFrame();
        desktop.addInternalFrame(frame);
        DesktopCouplingManager cm = new DesktopCouplingManager(desktop, frame);
        frame.setTitle("Coupling Manager");
        frame.setContentPane(cm);
//        frame.setSize(850, 420);
        frame.setResizable(true);
        frame.setClosable(true);
        frame.setMaximizable(true);
        frame.setIconifiable(true);
        frame.setVisible(true);
        frame.pack();
    }
}