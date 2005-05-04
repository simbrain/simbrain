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
import org.simnet.neurons.AdditiveNeuron;

public class AdditiveNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfLambda = new JTextField();
	private JTextField tfResistance = new JTextField();
	private JTextField tfTimeStep = new JTextField();
	
	public AdditiveNeuronPanel(){
		this.addItem("Activation", tfActivation);
		this.addItem("Increment", tfIncrement);	
		this.addItem("Lambda", tfLambda);
		this.addItem("Resistance", tfResistance);
		this.addItem("Time Step", tfTimeStep);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		AdditiveNeuron neuron_ref = (AdditiveNeuron)neuron_list.get(0);
		
		tfActivation.setText(Double.toString(neuron_ref.getActivation()));
		tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
		tfLambda.setText(Double.toString(neuron_ref.getLambda()));
		tfResistance.setText(Double.toString(neuron_ref.getResistance()));
		tfTimeStep.setText(Double.toString(neuron_ref.getTimeStep()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getActivation")) {
			tfActivation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getLambda")) {
			tfLambda.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getResistance")) {
			tfResistance.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getTimeStep")) {
			tfTimeStep.setText(NULL_STRING);
		}
	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
	for (int i = 0; i < neuron_list.size(); i++) {
		AdditiveNeuron neuron_ref = (AdditiveNeuron ) neuron_list.get(i);

		if (tfActivation.getText().equals(NULL_STRING) == false) {
			neuron_ref.setActivation(
				Double.parseDouble(tfActivation.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			neuron_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}
		if (tfLambda.getText().equals(NULL_STRING) == false) {
			neuron_ref.setLambda(
				Double.parseDouble(tfLambda.getText()));
		}
		if (tfResistance.getText().equals(NULL_STRING) == false) {
			neuron_ref.setResistance(
				Double.parseDouble(tfResistance.getText()));
		}
		if (tfTimeStep.getText().equals(NULL_STRING) == false) {
			neuron_ref.setTimeStep(
				Double.parseDouble(tfTimeStep.getText()));
		}
	}

   }

}
