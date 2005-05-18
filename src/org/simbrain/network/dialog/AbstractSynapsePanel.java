/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog;

import java.util.ArrayList;

import org.simbrain.util.LabelledItemPanel;

public abstract class AbstractSynapsePanel extends LabelledItemPanel {
	
	public static final String NULL_STRING = "...";

	protected ArrayList synapse_list; // The synapses being modified

	/**
	 * Populate fields with current data
	 */
	public abstract void fillFieldValues();

	 /**
	  * Called externally when the dialog is closed,
	  * to commit any changes made
	  */
	public abstract void commitChanges();

	/**
	 * @return Returns the synapse_list.
	 */
	public ArrayList getSynapse_list() {
		return synapse_list;
	}
	
	/**
	 * @param synapse_list
	 *            The synapse_list to set.
	 */
	public void setSynapse_list(ArrayList synapse_list) {
		this.synapse_list = synapse_list;
	}

}