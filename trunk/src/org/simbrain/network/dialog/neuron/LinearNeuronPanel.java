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
package org.simbrain.network.dialog.neuron;

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.LinearNeuron;

public class LinearNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfSlope = new JTextField();
    private JTextField tfBias = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel main_tab = new LabelledItemPanel();
	private RandomPanel rand_tab = new RandomPanel(true);
	private TristateDropDown isClipping = new TristateDropDown();
	private TristateDropDown isAddNoise = new TristateDropDown();
    
    public LinearNeuronPanel() {
        
        this.add(tabbedPane);
        main_tab.addItem("Slope", tfSlope);
        main_tab.addItem("Bias", tfBias);
        main_tab.addItem("Use clipping", isClipping);
        main_tab.addItem("Add noise", isAddNoise);
        tabbedPane.add(main_tab, "Main");
        tabbedPane.add(rand_tab, "Noise");
    }
    
    
    public void fillFieldValues(){
        LinearNeuron neuron_ref = (LinearNeuron)neuron_list.get(0);
        
	    isAddNoise.setSelected(neuron_ref.getAddNoise());
        tfSlope.setText(Double.toString(neuron_ref.getSlope()));
        tfBias.setText(Double.toString(neuron_ref.getBias()));
        isClipping.setSelected(neuron_ref.getClipping());
        isAddNoise.setSelected(neuron_ref.getAddNoise());
        
		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getSlope")) {
			tfSlope.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getClipping")){
		    isClipping.setNull();
		}
		if(!NetworkUtils.isConsistent(neuron_list, LinearNeuron.class, "getAddNoise")) {
		    isAddNoise.setNull();
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
        isClipping.setSelected(neuron_ref.getClipping());
        isAddNoise.setSelected(neuron_ref.getAddNoise());
        rand_tab.fillDefaultValues();
	}
    
	
    public void commitChanges() {
        for (int i = 0; i < neuron_list.size(); i++) {
            LinearNeuron neuron_ref = (LinearNeuron) neuron_list.get(i);

            if (tfSlope.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSlope(Double.parseDouble(tfSlope.getText()));
            }
            if (tfBias.getText().equals(NULL_STRING) == false) {
                neuron_ref.setBias(Double.parseDouble(tfBias.getText()));
            }
            if (isClipping.isNull() == false){
                neuron_ref.setClipping(isClipping.isSelected());
            }
            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }
            rand_tab.commitRandom(neuron_ref.getNoise());
        }
    }
}
