/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
 * <b>GaugeThread</b> updates the Gauge Panel; Used for repeatedly iterating
 * iterative projection algorithms.  Invoked by the "play" button on the
 * toolbar.
 */
public class GaugeThread extends Thread {

	private GaugePanel panelRef = null;
	private volatile boolean isRunning = false;
	
	Runnable updateNetwork = new Runnable() {
		public void run() {
			panelRef.iterate();
			panelRef.update();
		}
	};

	/**
	 * @param thePanel reference to the gauge panel
	 */
	public GaugeThread(GaugePanel thePanel) {
		panelRef = thePanel;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			while (isRunning == true) {
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
	public void setRunning(boolean b) {
		isRunning = b;
	}

}
