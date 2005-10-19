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

import org.simbrain.network.NetworkPanel;
/**
 * <b>ScriptThread</b> "runs" the network. It is controlled by the play and stop buttons in the 
 * network panel.
 */
public class ScriptThread extends Thread {

	private OdorWorld worldRef = null;
	String[][] values = null;
	private volatile boolean isRunning = false;

	public ScriptThread(OdorWorld wld, String[][] vals) {
		worldRef = wld;
		values = vals;
	}
	
	Runnable updateNetwork = new Runnable() {
		public void run() {
	  		for(int i = 0; i < worldRef.getCommandTargets().size(); i++) {
				NetworkPanel np = (NetworkPanel)worldRef.getCommandTargets().get(i);
				np.updateNetwork();
	  		}
		}
	};

	public void run() {
		try {
				for (int i = 0;i < values.length; i++) {
					if (isRunning == true) {
						for(int j = 0; i < worldRef.getCommandTargets().size(); j++) {
								NetworkPanel np = (NetworkPanel)worldRef.getCommandTargets().get(j);
								np.setUpdateCompleted(false);
								//System.out.println("" + values[i][0] + " " + values[i][1] + "  " + values[i][2]);
								//TODO: Make scripts able to handle multiple agents
								((OdorWorldAgent)worldRef.getAgentList().get(0)).moveTo(Integer.parseInt(values[i][0]),Integer.parseInt(values[i][1]),Integer.parseInt(values[i][2]));
								SwingUtilities.invokeLater(updateNetwork);
								worldRef.repaint();
								while (np.isUpdateCompleted()) {
									sleep(1);
								}
						}
					}
				}
				isRunning = false;
			}  catch (InterruptedException e) {
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
