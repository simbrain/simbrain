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

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.neurons.LinearNeuron;

public class LinearNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfSlope = new JTextField();
    private JTextField tfBias = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel main_tab = new LabelledItemPanel();
	private RandomPanel rand_tab = new RandomPanel();
    
    public LinearNeuronPanel(){
    	 this.add(tabbedPane);
    	 main_tab.addItem("Slope", tfSlope);
    	 main_tab.addItem("Bias", tfBias);
     tabbedPane.add(main_tab, "Main");
     tabbedPane.add(rand_tab, "Noise");
    }
    
    
    public void fillFieldValues(){
        LinearNeuron neuron_ref = (LinearNeuron)neuron_list.get(0);
        
        tfSlope.setText(Double.toString(neuron_ref.getSlope()));
        tfBias.setText(Double.toString(neuron_ref.getBias()));
        
		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getSlope")) {
			tfSlope.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}	
		
		rand_tab.fillFieldValues(getRandomizers());
		
    }

    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((LinearNeuron)neuron_list.get(i)).getNoise());
		}
		return ret;
}

	/**
	 * Fill field values to default values for linear neuron
	 *
	 */
	public void fillDefaultValues() {
        LinearNeuron neuron_ref = new LinearNeuron();        
        tfSlope.setText(Double.toString(neuron_ref.getSlope()));
        tfBias.setText(Double.toString(neuron_ref.getBias()));
        rand_tab.fillDefaultValues();
	}
    
	
    public void commitChanges(){
	    	for (int i = 0; i < neuron_list.size(); i++) {
	    		LinearNeuron neuron_ref = (LinearNeuron) neuron_list.get(i);
	
	    		if (tfSlope.getText().equals(NULL_STRING) == false) {
	    			neuron_ref.setSlope(
	    				Double.parseDouble(tfSlope.getText()));
	    		}
	    		if (tfBias.getText().equals(NULL_STRING) == false) {
	    			neuron_ref.setBias(
	    				Double.parseDouble(tfBias.getText()));
	    		}
	    		
	    		neuron_ref.setNoise(rand_tab.getRandomSource());
	    	}
    }
}
