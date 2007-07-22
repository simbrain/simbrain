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
package org.simbrain.workspace.couplingmanager;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JList;

import org.simbrain.workspace.Coupling;

/**
 * Handles key events for coupling manager.
 */
public class CouplingKeyAdapter extends KeyAdapter {

    /** Reference to coupling manager to access relevant commands. */
    private CouplingManager parent;

    /**
     * Constructor.
     *
     * @param parent coupling manager whose key events are being processed.
     */
    public CouplingKeyAdapter(final CouplingManager parent) {
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public void keyPressed(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
        case KeyEvent.VK_BACK_SPACE:        
            if (parent.getCouplingTray().hasFocus()) {
                parent.deleteSelectedCouplings();
            }
            break;
        default:
            break;
        }
    }
}
