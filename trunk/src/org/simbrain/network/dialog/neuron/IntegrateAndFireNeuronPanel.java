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
import org.simnet.interfaces.Network;
import org.simnet.neurons.IntegrateAndFireNeuron;


public class IntegrateAndFireNeuronPanel extends AbstractNeuronPanel {

	private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel mainTab = new LabelledItemPanel();

    private JTextField tfTimeConstant = new JTextField();
    private JTextField tfThreshold = new JTextField();
    private JTextField tfReset = new JTextField();
    private JTextField tfResistance = new JTextField();
    private JTextField tfRestingPotential = new JTextField();
    private JTextField tfTimeStep = new JTextField();
	private RandomPanel randTab = new RandomPanel(true);
	private TristateDropDown isClipping = new TristateDropDown();
	private TristateDropDown isAddNoise = new TristateDropDown();
    
    public IntegrateAndFireNeuronPanel(Network net){

		parentNet = net;
        
        this.add(tabbedPane);
        mainTab.addItem("Resistance", tfResistance);
        mainTab.addItem("Resting potential", tfRestingPotential);
        mainTab.addItem("Reset potential", tfReset);
        mainTab.addItem("Threshold", tfThreshold);
        mainTab.addItem("Time constant", tfTimeConstant);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }
    
    public void fillFieldValues(){
        IntegrateAndFireNeuron neuron_ref = (IntegrateAndFireNeuron)neuron_list.get(0);
		
		tfRestingPotential.setText(Double.toString(neuron_ref.getRestingPotential()));
		tfResistance.setText(Double.toString(neuron_ref.getResistance()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
		tfReset.setText(Double.toString(neuron_ref.getResetPotential()));
		tfThreshold.setText(Double.toString(neuron_ref.getThreshold()));
		tfTimeConstant.setText(Double.toString(neuron_ref.getTime_constant()));
		isAddNoise.setSelected(neuron_ref.getAddNoise());
		isClipping.setSelected(neuron_ref.getClipping());

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getRestingPotential")) {
			tfRestingPotential.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getResistance")) {
			tfResistance.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getTimeStep")) {
			tfTimeStep.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getClipping")){
		    isClipping.setNull();
		}
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getAddNoise")) {
		    isAddNoise.setNull();
		}
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getResetPotential")) {
			tfReset.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getTime_constant")) {
			tfTimeConstant.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getThreshold")) {
			tfThreshold.setText(NULL_STRING);
		}
		
		randTab.fillFieldValues(getRandomizers());
        
    }
    
    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((IntegrateAndFireNeuron)neuron_list.get(i)).getNoiseGenerator());
		}
		return ret;
    }
    
    public void fillDefaultValues(){
        IntegrateAndFireNeuron neuronRef = new IntegrateAndFireNeuron();
		tfRestingPotential.setText(Double.toString(neuronRef.getRestingPotential()));
		tfResistance.setText(Double.toString(neuronRef.getResistance()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
		tfReset.setText(Double.toString(neuronRef.getResetPotential()));
		tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
		tfTimeConstant.setText(Double.toString(neuronRef.getTime_constant()));
		isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }
    
    public void commitChanges(){
        
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));
        
        for (int i = 0; i < neuron_list.size(); i++) {
            IntegrateAndFireNeuron neuronRef = (IntegrateAndFireNeuron) neuron_list.get(i);

            if (tfResistance.getText().equals(NULL_STRING) == false) {
                neuronRef.setResistance(Double
                        .parseDouble(tfResistance.getText()));
            }
            if (tfRestingPotential.getText().equals(NULL_STRING) == false) {
                neuronRef.setRestingPotential(Double.parseDouble(tfRestingPotential
                        .getText()));
            }
            if (tfTimeStep.getText().equals(NULL_STRING) == false) {
                neuronRef.setTimeStep(Double.parseDouble(tfTimeStep
                        .getText()));
            }
            if (isClipping.isNull() == false){
                neuronRef.setClipping(isClipping.isSelected());
            }
            if (isAddNoise.isNull() == false) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }
            if (tfReset.getText().equals(NULL_STRING) == false) {
                neuronRef.setResetPotential(Double.parseDouble(tfReset
                        .getText()));
            }
            if (tfThreshold.getText().equals(NULL_STRING) == false) {
                neuronRef.setThreshold(Double.parseDouble(tfThreshold
                        .getText()));
            }
            if (tfTimeConstant.getText().equals(NULL_STRING) == false) {
                neuronRef.setTime_constant(Double.parseDouble(tfTimeConstant
                        .getText()));
            }
            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }
}

