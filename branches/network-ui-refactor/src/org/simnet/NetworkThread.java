/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet;

import javax.swing.SwingUtilities;

import org.simnet.interfaces.Network;


/**
 * <b>NetworkThread</b> "runs" the network. It is controlled by the play and stop buttons in the  network panel.
 */
public class NetworkThread extends Thread {
    
    /** Reference to NetworkPanel. */
    private Network networkRef = null;
    
    /** Whether this thread is running or not. */
    private volatile boolean isRunning = false;
    
    /**
     * Updated the network.
     */
    Runnable updateNetwork = new Runnable() {
            public void run() {
                networkRef.updateTopLevel();
            }
        };

    /**
     * @param thePanel
     */
    public NetworkThread(final Network network) {
        networkRef = network;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            while (isRunning == true) {
                networkRef.setUpdateCompleted(false);

                // SwingUtilities.invokeLater(updateGraphics);
                SwingUtilities.invokeLater(updateNetwork);

                while (!networkRef.isUpdateCompleted()) {
                    sleep(10);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true if the thread is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @param b true to run the network thread, false to stop it
     */
    public void setRunning(final boolean b) {
        isRunning = b;
    }
}
