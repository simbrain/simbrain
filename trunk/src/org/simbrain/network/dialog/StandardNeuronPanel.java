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
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.StandardNeuron;
/**
 * 
 */

public class StandardNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JTextField tfDecay = new JTextField();
	private JTextField tfBias = new JTextField();
	private JComboBox cbActivationRule = new JComboBox(ActivationRule.getList());
	
	public StandardNeuronPanel(){
		this.addItem("Activation function", cbActivationRule);
		this.addItem("Upper bound", tfUpBound);
		this.addItem("Lower bound", tfLowBound);
		this.addItem("Bias", tfBias);
		this.addItem("Decay", tfDecay);	
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		StandardNeuron neuron_ref = (StandardNeuron)neuron_list.get(0);
		
		cbActivationRule.setSelectedIndex(ActivationRule.getActivationFunctionIndex(neuron_ref.getActivationFunction().getName()));

		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		tfDecay.setText(Double.toString(neuron_ref.getDecay()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getActivationFunctionS")) {
			if((cbActivationRule.getItemCount() == ActivationRule.getList().length)) {				
				cbActivationRule.addItem(NULL_STRING);				
			}
			cbActivationRule.setSelectedIndex(ActivationRule.getList().length);
		}
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getDecay")) {
			tfDecay.setText(NULL_STRING);
		}	
	}
	
	/**
	 * Fill field values to default values for standard neurons
	 *
	 */
	public void fillDefaultValues() {
		StandardNeuron neuron_ref = new StandardNeuron();
		cbActivationRule.setSelectedIndex(ActivationRule.getActivationFunctionIndex(neuron_ref.getActivationFunction().getName()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		tfDecay.setText(Double.toString(neuron_ref.getDecay()));
	}

	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {

		for (int i = 0; i < neuron_list.size(); i++) {
			StandardNeuron neuron_ref = (StandardNeuron) neuron_list.get(i);

			if (cbActivationRule.getSelectedItem().equals(NULL_STRING) == false) {
				neuron_ref.setActivationFunction(ActivationRule
						.getActivationFunction(cbActivationRule
								.getSelectedItem().toString()));
			}

			if (tfUpBound.getText().equals(NULL_STRING) == false) {
				neuron_ref.setUpperBound(Double
						.parseDouble(tfUpBound.getText()));
			}
			if (tfLowBound.getText().equals(NULL_STRING) == false) {
				neuron_ref.setLowerBound(Double.parseDouble(tfLowBound
						.getText()));
			}
			if (tfDecay.getText().equals(NULL_STRING) == false) {
				neuron_ref.setDecay(Double.parseDouble(tfDecay.getText()));
			}
			if (tfBias.getText().equals(NULL_STRING) == false) {
				neuron_ref.setBias(Double.parseDouble(tfBias.getText()));
			}

		}

	}

}
