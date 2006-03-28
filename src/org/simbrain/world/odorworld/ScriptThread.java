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
package org.simbrain.world.odorworld;

import javax.swing.SwingUtilities;



/**
 * <b>ScriptThread</b> "runs" the network. It is controlled by the play and stop buttons in the  network panel.
 */
public class ScriptThread extends Thread {

    /** World reference. */
    private OdorWorld worldRef = null;

    /** Two dimensional values array. */
    private String[][] values = null;

    /** Script thread running. */
    private volatile boolean isRunning = false;

    /**
     * Script thread.
     * @param wld World
     * @param vals Values
     */
    public ScriptThread(final OdorWorld wld, final String[][] vals) {
        worldRef = wld;
        setValues(vals);
    }

    /**
     * Updates the network.
     */
    private Runnable updateNetwork = new Runnable() {
            public void run() {
                worldRef.fireWorldChanged();
            }
        };

    /**
     * @see java.lang.Thread.run
     */
    public void run() {
        for (int i = 0; i < getValues().length; i++) {
            if (isRunning) {

                // System.out.println("" + values[i][0] + " " + values[i][1] + "
                // " + values[i][2]);
                ((OdorWorldAgent) worldRef.getAgentList().get(0)).moveTo(
                        Integer.parseInt(getValues()[i][0]),
                        Integer.parseInt(getValues()[i][1]),
                        Integer.parseInt(getValues()[i][2]));
                SwingUtilities.invokeLater(getUpdateNetwork());
                worldRef.fireWorldChanged();
                worldRef.repaint();

            }
        }
        isRunning = false;
    }

    /**
     * @return true if the thread is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @param b
     *            true to run the network thread, false to stop it
     */
    public void setRunning(final boolean b) {
        isRunning = b;
    }

    /**
     * @param values The values to set.
     */
    void setValues(final String[][] values) {
        this.values = values;
    }

    /**
     * @return Returns the values.
     */
    String[][] getValues() {
        return values;
    }

    /**
     * @param updateNetwork The updateNetwork to set.
     */
    void setUpdateNetwork(final Runnable updateNetwork) {
        this.updateNetwork = updateNetwork;
    }

    /**
     * @return Returns the updateNetwork.
     */
    Runnable getUpdateNetwork() {
        return updateNetwork;
    }
}
