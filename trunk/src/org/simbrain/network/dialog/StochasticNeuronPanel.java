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
package org.simbrain.network.dialog;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.neurons.StochasticNeuron;


public class StochasticNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfUpperValue = new JTextField();
    private JTextField tfLowerValue = new JTextField();
    private JTextField tfFiringProbability = new JTextField();
    
    public StochasticNeuronPanel(){
        
        this.addItem("Upper value (\"Firing\")", tfUpperValue);
        this.addItem("Lower Value (\"Not firing\")", tfLowerValue);
        this.addItem("Firing probability", tfFiringProbability);
    }
    
    public void fillFieldValues(){
		StochasticNeuron neuron_ref = (StochasticNeuron)neuron_list.get(0);
		
		tfLowerValue.setText(Double.toString(neuron_ref.getLowerValue()));
		tfUpperValue.setText(Double.toString(neuron_ref.getUpperValue()));
		tfFiringProbability.setText(Double.toString(neuron_ref.getFiringProbability()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, StochasticNeuron.class, "getLowerValue")) {
			tfLowerValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StochasticNeuron.class, "getUpperValue")) {
			tfUpperValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StochasticNeuron.class, "getFiringProbability")) {
			tfFiringProbability.setText(NULL_STRING);
		}
        
    }
    
    public void fillDefaultValues(){
        StochasticNeuron neuronRef = new StochasticNeuron();
		tfLowerValue.setText(Double.toString(neuronRef.getLowerValue()));
		tfUpperValue.setText(Double.toString(neuronRef.getUpperValue()));
		tfFiringProbability.setText(Double.toString(neuronRef.getFiringProbability()));
    }
    
    public void commitChanges(){
        
        for (int i = 0; i < neuron_list.size(); i++) {
            StochasticNeuron neuronRef = (StochasticNeuron) neuron_list.get(i);

            if (tfUpperValue.getText().equals(NULL_STRING) == false) {
                neuronRef.setUpperBound(Double
                        .parseDouble(tfUpperValue.getText()));
            }
            if (tfLowerValue.getText().equals(NULL_STRING) == false) {
                neuronRef.setLowerBound(Double.parseDouble(tfLowerValue
                        .getText()));
            }
            if (tfFiringProbability.getText().equals(NULL_STRING) == false) {
                neuronRef.setFiringProbability(Double.parseDouble(tfFiringProbability
                        .getText()));
            }
        }
    }
}
