/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simbrain.gauge.graphics;

import javax.swing.SwingUtilities;


/**
 * <b>GaugeThread</b> updates the Gauge Panel; Used for repeatedly iterating iterative projection algorithms.  Invoked
 * by the "play" button on the toolbar.
 */
public class GaugeThread extends Thread {
    /** Gauge panel. */
    private GaugePanel panelRef = null;
    /** Is method running. */
    private volatile boolean isRunning = false;
    /** Update network. */
    private Runnable updateNetwork = new Runnable() {
            public void run() {
                panelRef.iterate();
                panelRef.update();
            }
        };

    /**
     * @param thePanel reference to the gauge panel
     */
    public GaugeThread(final GaugePanel thePanel) {
        panelRef = thePanel;
    }

    /**
     * Runs thread.
     */
    public void run() {
        try {
            while (isRunning) {
                panelRef.setUpdateCompleted(false);
                SwingUtilities.invokeLater(updateNetwork);

                while (!panelRef.isUpdateCompleted()) {
                    sleep(5);
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

    /**
     * @param updateNetwork Set update network while running.
     */
    public void setUpdateNetwork(final Runnable updateNetwork) {
        this.updateNetwork = updateNetwork;
    }

    /**
     * @return updateNetwork.
     */
    public Runnable getUpdateNetwork() {
        return this.updateNetwork;
    }
}
