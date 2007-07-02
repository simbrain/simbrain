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
package org.simbrain.workspace;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import org.simbrain.network.actions.SelectIncomingWeightsAction;
import org.simbrain.network.actions.SelectOutgoingWeightsAction;
import org.simbrain.network.actions.connection.ConnectNeuronsAction;
import org.simbrain.workspace.couplingmanager.CouplingManager;

/**
 * Network key adapter.
 */
class WorkspaceKeyAdapter extends KeyAdapter {

    /**
     * Responds to key pressed events.
     *
     * @param e Key event
     */
    public void keyPressed(final KeyEvent e) {
        int keycode = e.getKeyCode();
        switch (keycode) {
        //TODO: For Testing.   This stuff should mostly be in actions.
        case KeyEvent.VK_M:
            JFrame frame = new JFrame();
            CouplingManager cm = new CouplingManager();        
            frame.setContentPane(cm);
            frame.setSize(850, 420);
            frame.setLocationRelativeTo(null);
            frame.pack();
            frame.setResizable(false); // Maybe should allow this...    
            frame.setVisible(true);
            break;
        case KeyEvent.VK_U:
            Workspace.getInstance().globalUpdate();
            break;
        default:
            break;
        }
    }


}
