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

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.BinaryNeuron;

public class BinaryNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfUpValue = new JTextField();
	private JTextField tfLowValue = new JTextField();
	private JTextField tfThreshold = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel mainTab = new LabelledItemPanel();
	private RandomPanel randTab = new RandomPanel();
	private TristateDropDown isAddNoise = new TristateDropDown();
	
	public BinaryNeuronPanel(){
	    
	    this.add(tabbedPane);
		mainTab.addItem("Threshold", tfThreshold);
		mainTab.addItem("Upper value", tfUpValue);
		mainTab.addItem("Lower value", tfLowValue);
		mainTab.addItem("Add Noise", isAddNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		BinaryNeuron neuron_ref = (BinaryNeuron)neuron_list.get(0);
		
		tfLowValue.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpValue.setText(Double.toString(neuron_ref.getUpperBound()));
		tfThreshold.setText(Double.toString(neuron_ref.getThreshold()));
		isAddNoise.setSelected(neuron_ref.isAddNoise());

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getLowerBound")) {
			tfLowValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getUpperBound")) {
			tfUpValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getThreshold")) {
			tfThreshold.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "isAddNoise")){
		    isAddNoise.setNull();
		}
		randTab.fillFieldValues(getRandomizers());
	}
	
    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((BinaryNeuron)neuron_list.get(i)).getNoise());
		}
		return ret;
    }
	
	/**
	 * Fill field values to default values for binary neuron
	 *
	 */
	public void fillDefaultValues() {
		BinaryNeuron neuron_ref = new BinaryNeuron();
		tfLowValue.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpValue.setText(Double.toString(neuron_ref.getUpperBound()));
		tfThreshold.setText(Double.toString(neuron_ref.getThreshold()));
		isAddNoise.setSelected(neuron_ref.isAddNoise());
		randTab.fillDefaultValues();
	}
	
    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            BinaryNeuron neuron_ref = (BinaryNeuron) neuron_list.get(i);

            if (tfUpValue.getText().equals(NULL_STRING) == false) {
                neuron_ref.setUpperBound(Double
                        .parseDouble(tfUpValue.getText()));
            }
            if (tfLowValue.getText().equals(NULL_STRING) == false) {
                neuron_ref.setLowerBound(Double.parseDouble(tfLowValue
                        .getText()));
            }
            if (tfThreshold.getText().equals(NULL_STRING) == false) {
                neuron_ref.setThreshold(Double.parseDouble(tfThreshold
                        .getText()));
            }
            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoise());
        }

    }

}
