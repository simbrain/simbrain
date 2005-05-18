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

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.interfaces.Synapse;
/**
 * Contains fields that are constant between different synapse types.  Used when a mixed set
 * of synapses are selected
 */

public class MixedSynapsePanel extends AbstractSynapsePanel {
	
	private JTextField tfStrength = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	
	public MixedSynapsePanel(){
		this.addItem("Strength", tfStrength);
		this.addItem("Upper bound", tfUpBound);
		this.addItem("Lower bound", tfLowBound);
		this.addItem("Increment", tfIncrement);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		Synapse synapse_ref = (Synapse)synapse_list.get(0);
		
		tfStrength.setText(Double.toString(synapse_ref.getStrength()));
		tfLowBound.setText(Double.toString(synapse_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(synapse_ref.getUpperBound()));
		tfIncrement.setText(Double.toString(synapse_ref.getIncrement()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getStrength")) {
			tfStrength.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}	

	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
	for (int i = 0; i < synapse_list.size(); i++) {
		Synapse synapse_ref = (Synapse) synapse_list.get(i);

		if (tfStrength.getText().equals(NULL_STRING) == false) {
			synapse_ref.setStrength(
				Double.parseDouble(tfStrength.getText()));
		}
		if (tfUpBound.getText().equals(NULL_STRING) == false) {
			synapse_ref.setUpperBound(
				Double.parseDouble(tfUpBound.getText()));
		}
		if (tfLowBound.getText().equals(NULL_STRING) == false) {
			synapse_ref.setLowerBound(
				Double.parseDouble(tfLowBound.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			synapse_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}

	   	
	}

   }

}
