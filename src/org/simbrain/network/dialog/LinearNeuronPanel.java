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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.LinearNeuron;

public class LinearNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfSlope = new JTextField();
    private JTextField tfMidpoint = new JTextField();
    private JTextField tfDecayRate = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel main_tab = new LabelledItemPanel();
	private RandomPanel rand_tab = new RandomPanel();
    
    public LinearNeuronPanel(){
    	 this.add(tabbedPane);
    	 main_tab.addItem("Slope", tfSlope);
    	 main_tab.addItem("Midpoint", tfMidpoint);
    	 main_tab.addItem("Decay rate", tfDecayRate);
     tabbedPane.add(main_tab, "Main");
     tabbedPane.add(rand_tab, "Noise");
    }
    
    
    public void fillFieldValues(){
        LinearNeuron neuron_ref = (LinearNeuron)neuron_list.get(0);
        
        tfSlope.setText(Double.toString(neuron_ref.getSlope()));
        tfMidpoint.setText(Double.toString(neuron_ref.getMidpoint()));
        tfDecayRate.setText(Double.toString(neuron_ref.getDecayRate()));
        
		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getSlope")) {
			tfSlope.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getMidpoint")) {
			tfMidpoint.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getDecayRate")) {
			tfDecayRate.setText(NULL_STRING);
		}
    }
    
    public void commitChanges(){
    	for (int i = 0; i < neuron_list.size(); i++) {
    		LinearNeuron neuron_ref = (LinearNeuron) neuron_list.get(i);

    		if (tfSlope.getText().equals(NULL_STRING) == false) {
    			neuron_ref.setSlope(
    				Double.parseDouble(tfSlope.getText()));
    		}
    		if (tfMidpoint.getText().equals(NULL_STRING) == false) {
    			neuron_ref.setMidpoint(
    				Double.parseDouble(tfMidpoint.getText()));
    		}
    		if (tfDecayRate.getText().equals(NULL_STRING) == false) {
    			neuron_ref.setDecayRate(
    				Double.parseDouble(tfDecayRate.getText()));
    		}
    	}
    }
}
