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

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.ActivationRule;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.SigmoidalNeuron;

public class SigmoidalNeuronPanel extends AbstractNeuronPanel {

    private JComboBox cbImplementation = new JComboBox(SigmoidalNeuron.getFunctionList());
    private JTextField tfBias = new JTextField();
    private JTextField tfSlope = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel mainTab = new LabelledItemPanel();
	private RandomPanel randTab = new RandomPanel(true);
	private TristateDropDown isClipping = new TristateDropDown();
	private TristateDropDown isAddNoise = new TristateDropDown();
    
    public SigmoidalNeuronPanel(){
        
        this.add(tabbedPane);
        mainTab.addItem("Implementation", cbImplementation);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Use clipping", isClipping);
		mainTab.addItem("Add noise", isAddNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
    }
    
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		SigmoidalNeuron neuron_ref = (SigmoidalNeuron)neuron_list.get(0);
		
		cbImplementation.setSelectedIndex(neuron_ref.getImplementationIndex());
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		tfSlope.setText(Double.toString(neuron_ref.getSlope()));
		

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getImplementationIndex")) {
			if((cbImplementation.getItemCount() == SigmoidalNeuron.getFunctionList().length)) {				
				cbImplementation.addItem(NULL_STRING);				
			}
			cbImplementation.setSelectedIndex(SigmoidalNeuron.getFunctionList().length);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getSlope")) {
			tfSlope.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getClipping")){
		    isClipping.setNull();
		}
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getAddNoise")){
		    isAddNoise.setNull();
		}
		randTab.fillFieldValues(getRandomizers());

	}
    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((SigmoidalNeuron)neuron_list.get(i)).getNoiseGenerator());
		}
		return ret;
    }
	
	/**
	 * Fill field values to default values for sigmoidal neuron
	 *
	 */
	public void fillDefaultValues() {
		SigmoidalNeuron neuron_ref = new SigmoidalNeuron();
		
		cbImplementation.setSelectedIndex(neuron_ref.getImplementationIndex());
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		tfSlope.setText(Double.toString(neuron_ref.getSlope()));
		isClipping.setSelected(neuron_ref.getClipping());
		isAddNoise.setSelected(neuron_ref.getAddNoise());
		randTab.fillDefaultValues();
	}

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            SigmoidalNeuron neuron_ref = (SigmoidalNeuron) neuron_list.get(i);

            if (cbImplementation.getSelectedItem().equals(NULL_STRING) == false) {
                neuron_ref.setImplementationIndex(cbImplementation.getSelectedIndex());
            }
            if (tfBias.getText().equals(NULL_STRING) == false) {
                neuron_ref.setBias(Double.parseDouble(tfBias
                        .getText()));
            }
            if (tfSlope.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSlope(Double.parseDouble(tfSlope.getText()));
            }
            if (isClipping.isNull() == false){
                neuron_ref.setClipping(isClipping.isSelected());
            }
            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoiseGenerator());
        }
    }
}
