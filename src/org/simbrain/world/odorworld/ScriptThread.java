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

    /** Keeps tabs of update index. */
    private int iterationNumber = 0;

    private DialogScript dialog = null;

    /**
     * Script thread.
     * @param wld World
     * @param vals Values
     */
    public ScriptThread(final OdorWorld wld, final String[][] vals, DialogScript dialog) {
        worldRef = wld;
        values = vals;
        this.dialog = dialog;
    }

    /**
     * Update the world.
     */
    private Runnable updateScript = new Runnable() {
            public void run() {
               ((OdorWorldEntity) worldRef.getEntityList().get(Integer.parseInt(values[iterationNumber][0]))).moveTo(
                                Integer.parseInt(values[iterationNumber][1]),
                                Integer.parseInt(values[iterationNumber][2]));

               iterationNumber++;
               dialog.setIterationNumber(iterationNumber);
               if (iterationNumber > values.length) {
                   iterationNumber = 0;
                   isRunning = false;
               }

            }
        };

    /**
     * @see java.lang.Thread.run
     */
    public void run() {
        try {
            while (isRunning) {
                worldRef.setUpdateCompleted(false);
                SwingUtilities.invokeLater(updateScript);
                while (!worldRef.isUpdateCompleted()) {
                    sleep(1);
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
