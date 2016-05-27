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

import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Reposition and resize all desktop windows in the upper left corner. Useful
 * when they get "lost".
 */
public final class RepositionAllWindowsAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;
    
    //TODO: Make this use all internal frames (see SimbrainDesktop.addInternalFrame)
    //  And also coordinate spacing with the logic in SimbrainDesktop.addDesktopComponent()

    /** Reference to Simbrain Desktop. */
    private SimbrainDesktop desktop;

    /**
     * Construct the action.
     * 
     * @param desktop
     */
    public RepositionAllWindowsAction(final SimbrainDesktop desktop) {
        super("Reposition All Windows", desktop.getWorkspace());
        putValue(SHORT_DESCRIPTION,
                "Repositions and resize all windows. Useful when windows get \"lost\" offscreen.");
        this.desktop = desktop;
    }

    /**
     * @see AbstractAction
     * @param event
     */
    public void actionPerformed(final ActionEvent event) {
        int i = 0;
        for (GuiComponent<?> c : desktop.getDesktopComponents()) {
            int height = (int) desktop.getFrame().getSize().getHeight();
            int width = (int) desktop.getFrame().getSize().getWidth();
            c.getParentFrame().setBounds(i * 15, i * 15, width / 2, height / 2);
            c.getParentFrame().toFront();
            i++;
        }
    }
}