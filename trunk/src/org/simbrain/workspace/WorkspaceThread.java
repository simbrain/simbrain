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
package org.simbrain.workspace;

import javax.swing.SwingUtilities;

/**
 * <b>NetworkThread</b> "runs" the network. It is controlled by the play and stop buttons
 * in the  network panel.
 */
public class WorkspaceThread extends Thread {
    /** The time to sleep between updates. */
    private static final int SLEEP_INTERVAL = 10;
    
    /** The parent workspace. */
    private final Workspace workspace;
    
    /**
     * Creates a new instance.
     * 
     * @param workspace The parent Workspace.
     */
    public WorkspaceThread(final Workspace workspace) {
        this.workspace = workspace;
    }
    
    /** Whether this thread is running or not. */
    private volatile boolean isRunning = false;

    /**
     * Updated the network.
     */
    private Runnable updateNetwork = new Runnable() {
        public void run() {
            workspace.globalUpdate();
        }
    };
    
    /**
     * Updated the network.
     */
    private Runnable stopNetwork = new Runnable() {
        public void run() {
            workspace.updateStopped();
        }
    };
    
    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            while (isRunning) {
                workspace.setUpdateCompleted(false);

                // SwingUtilities.invokeLater(updateGraphics);
                SwingUtilities.invokeLater(updateNetwork);

                while (!workspace.isUpdateCompleted()) {
                    sleep(SLEEP_INTERVAL);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeLater(stopNetwork);
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
