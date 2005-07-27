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
import org.simnet.neurons.BinaryNeuron;

public class BinaryNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfUpValue = new JTextField();
	private JTextField tfLowValue = new JTextField();
	private JTextField tfThreshold = new JTextField();
	
	public BinaryNeuronPanel(){
		this.addItem("Threshold", tfThreshold);
		this.addItem("Upper value", tfUpValue);
		this.addItem("Lower value", tfLowValue);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		BinaryNeuron neuron_ref = (BinaryNeuron)neuron_list.get(0);
		
		tfLowValue.setText(Double.toString(neuron_ref.getLowerValue()));
		tfUpValue.setText(Double.toString(neuron_ref.getUpperValue()));
		tfThreshold.setText(Double.toString(neuron_ref.getThreshold()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getLowerValue")) {
			tfLowValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getUpperValue")) {
			tfUpValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getThreshold")) {
			tfThreshold.setText(NULL_STRING);
		}
	}
	
    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            BinaryNeuron neuron_ref = (BinaryNeuron) neuron_list.get(i);

            if (tfUpValue.getText().equals(NULL_STRING) == false) {
                neuron_ref.setUpperValue(Double
                        .parseDouble(tfUpValue.getText()));
            }
            if (tfLowValue.getText().equals(NULL_STRING) == false) {
                neuron_ref.setLowerValue(Double.parseDouble(tfLowValue
                        .getText()));
            }
            if (tfThreshold.getText().equals(NULL_STRING) == false) {
                neuron_ref.setThreshold(Double.parseDouble(tfThreshold
                        .getText()));
            }
        }

    }

}
