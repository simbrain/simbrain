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
import javax.swing.JInternalFrame;

import org.simbrain.trainer.TrainerGUI;
import org.simbrain.workspace.gui.GenericJInternalFrame;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.couplingmanager2.DesktopCouplingManager;

/**
 * Open data world in current workspace.
 */
public final class NewTrainerAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private SimbrainDesktop desktop;

    /**
     * Create an open data world with the specified
     * workspace.
     */
    public NewTrainerAction(SimbrainDesktop desktop) {
        super("Add Network Trainer");
        //putValue(SMALL_ICON, ResourceManager.getImageIcon("Properties.png")); TODO
        this.desktop = desktop;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        TrainerGUI trainer = new TrainerGUI(desktop.getWorkspace());
        JInternalFrame frame = new JInternalFrame();
        frame.setTitle("Network Trainer");
        frame.setContentPane(trainer);
        frame.setResizable(true);
        frame.setClosable(true);
        frame.setMaximizable(true);
        frame.setIconifiable(true);
        frame.setVisible(true);
        frame.pack();
        desktop.addInternalFrame(frame);
    }
}