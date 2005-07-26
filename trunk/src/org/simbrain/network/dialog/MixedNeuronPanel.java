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

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.Neuron;
/**
 * Contains fields that are constant between different neuron types.  Used when a mixed set
 * of neurons are selected
 */

public class MixedNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	
	public MixedNeuronPanel(){
		this.addItem("Activation", tfActivation);
		this.addItem("Upper bound", tfUpBound);
		this.addItem("Lower bound", tfLowBound);
		this.addItem("Increment", tfIncrement);	
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		Neuron neuron_ref = (Neuron)neuron_list.get(0);
		
		tfActivation.setText(Double.toString(neuron_ref.getActivation()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));
		tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getActivation")) {
			tfActivation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}	

	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
	for (int i = 0; i < neuron_list.size(); i++) {
		Neuron neuron_ref = (Neuron) neuron_list.get(i);

		if (tfActivation.getText().equals(NULL_STRING) == false) {
			neuron_ref.setActivation(
				Double.parseDouble(tfActivation.getText()));
		}
		if (tfUpBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setUpperBound(
				Double.parseDouble(tfUpBound.getText()));
		}
		if (tfLowBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setLowerBound(
				Double.parseDouble(tfLowBound.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			neuron_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}
	}

   }

}
