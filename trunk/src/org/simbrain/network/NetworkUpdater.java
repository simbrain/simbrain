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
package org.simbrain.network;

import javax.swing.SwingUtilities;

import org.simbrain.network.interfaces.RootNetwork;

/**
 * Task for updating the network, using SwingUtilities.invokeLater (required for
 * Gui)
 * 
 * TODO: Should this be in the Gui sub-package with a separate task for general
 * network update?
 */
public class NetworkUpdater implements Runnable {

	/** Reference to network component. */
	private RootNetwork root;

	/**
	 * @param component
	 */
	public NetworkUpdater(RootNetwork root) {
		super();
		this.root = root;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
			try {
				root.setUpdateCompleted(false);
				SwingUtilities.invokeLater(new UpdateNetwork());
				while (!root.isUpdateCompleted()) {
					Thread.sleep(1); // TODO: make this settable? 
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Updated the network.
	 */
	private class UpdateNetwork implements Runnable {
		public void run() {
			root.updateRootNetwork();
		}
	}

}
