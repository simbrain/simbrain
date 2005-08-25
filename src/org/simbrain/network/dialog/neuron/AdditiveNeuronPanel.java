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
package org.simbrain.network.dialog.neuron;

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.Network;
import org.simnet.networks.StandardNetwork;
import org.simnet.neurons.AdditiveNeuron;

public class AdditiveNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfLambda = new JTextField();
	private JTextField tfResistance = new JTextField();
	private JTextField tfTimeStep = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel mainTab = new LabelledItemPanel();
	private RandomPanel randTab = new RandomPanel(true);
	private TristateDropDown isClipping = new TristateDropDown();
	private TristateDropDown isAddNoise = new TristateDropDown();
	
	public AdditiveNeuronPanel(){
	    
	    this.add(tabbedPane);
		mainTab.addItem("Lambda", tfLambda);
		mainTab.addItem("Resistance", tfResistance);
		mainTab.addItem("Time step", tfTimeStep);
		mainTab.addItem("Use clipping", isClipping);
		mainTab.addItem("Add noise", isAddNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		AdditiveNeuron neuron_ref = (AdditiveNeuron)neuron_list.get(0);
		
		tfLambda.setText(Double.toString(neuron_ref.getLambda()));
		tfResistance.setText(Double.toString(neuron_ref.getResistance()));
		tfTimeStep.setText(Double.toString(neuron_ref.getParentNetwork().getTimeStep()));
        isClipping.setSelected(neuron_ref.getClipping());
		isAddNoise.setSelected(neuron_ref.getAddNoise());

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getLambda")) {
			tfLambda.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getResistance")) {
			tfResistance.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getClipping")){
		    isClipping.setNull();
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getAddNoise")) {
		    isAddNoise.setNull();
		}
		randTab.fillFieldValues(getRandomizers());
	}
	
    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((AdditiveNeuron)neuron_list.get(i)).getNoiseGenerator());
		}
		return ret;
    }

	/**
	 * Fill field values to default values for additive neuron
	 *
	 */
	public void fillDefaultValues() {
		AdditiveNeuron neuron_ref = new AdditiveNeuron();
        Network tmpNet = new StandardNetwork();
		tfLambda.setText(Double.toString(neuron_ref.getLambda()));
		tfResistance.setText(Double.toString(neuron_ref.getResistance()));
		tfTimeStep.setText(Double.toString(tmpNet.getTimeStep()));
		isClipping.setSelected(neuron_ref.getClipping());
		isAddNoise.setSelected(neuron_ref.getAddNoise());
        randTab.fillDefaultValues();
	}
	
	
    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            AdditiveNeuron neuron_ref = (AdditiveNeuron) neuron_list.get(i);

            if (tfLambda.getText().equals(NULL_STRING) == false) {
                neuron_ref.setLambda(Double.parseDouble(tfLambda.getText()));
            }
            if (tfResistance.getText().equals(NULL_STRING) == false) {
                neuron_ref.setResistance(Double.parseDouble(tfResistance
                        .getText()));
            }
            if (tfTimeStep.getText().equals(NULL_STRING) == false) {
                neuron_ref.getParentNetwork().setTimeStep(Double
                        .parseDouble(tfTimeStep.getText()));
            }
            if(isAddNoise.isNull() == false){
                neuron_ref.setClipping(isClipping.isSelected());
            }
            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoiseGenerator());
        }

    }

}
