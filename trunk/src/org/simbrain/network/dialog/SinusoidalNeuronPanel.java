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
import org.simnet.neurons.SinusoidalNeuron;


public class SinusoidalNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfPhase = new JTextField();
    private JTextField tfFrequency = new JTextField();
    
    public SinusoidalNeuronPanel(){
        
        this.addItem("Phase", tfPhase);
        this.addItem("Frequency", tfFrequency);

    }
    
    public void fillFieldValues(){
        SinusoidalNeuron neuron_ref = (SinusoidalNeuron)neuron_list.get(0);
		
		tfFrequency.setText(Double.toString(neuron_ref.getFrequency()));
		tfPhase.setText(Double.toString(neuron_ref.getPhase()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, SinusoidalNeuron.class, "getFrequency")) {
			tfFrequency.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SinusoidalNeuron.class, "getPhase")) {
			tfPhase.setText(NULL_STRING);
		}	
        
    }
    
    public void fillDefaultValues(){
        SinusoidalNeuron neuronRef = new SinusoidalNeuron();
		tfFrequency.setText(Double.toString(neuronRef.getFrequency()));
		tfPhase.setText(Double.toString(neuronRef.getPhase()));
    }
    
    public void commitChanges(){
        
        for (int i = 0; i < neuron_list.size(); i++) {
            SinusoidalNeuron neuronRef = (SinusoidalNeuron) neuron_list.get(i);

            if (tfPhase.getText().equals(NULL_STRING) == false) {
                neuronRef.setUpperBound(Double
                        .parseDouble(tfPhase.getText()));
            }
            if (tfFrequency.getText().equals(NULL_STRING) == false) {
                neuronRef.setLowerBound(Double.parseDouble(tfFrequency
                        .getText()));
            }
        }
    }
}

