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
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.IntegrateAndFireNeuron;
import org.simnet.neurons.LinearNeuron;


public class IntegrateAndFireNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfResistance = new JTextField();
    private JTextField tfRestingPotential = new JTextField();
    private JTextField tfTimeStep = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel mainTab = new LabelledItemPanel();
	private RandomPanel randTab = new RandomPanel();
	private TristateDropDown isAddNoise = new TristateDropDown();
    
    public IntegrateAndFireNeuronPanel(){
        
        this.add(tabbedPane);
        mainTab.addItem("Resistance", tfResistance);
        mainTab.addItem("Resting potential", tfRestingPotential);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }
    
    public void fillFieldValues(){
        IntegrateAndFireNeuron neuron_ref = (IntegrateAndFireNeuron)neuron_list.get(0);
		
		tfRestingPotential.setText(Double.toString(neuron_ref.getResistance()));
		tfResistance.setText(Double.toString(neuron_ref.getRestingPotential()));
		tfTimeStep.setText(Double.toString(neuron_ref.getTimeStep()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getLowerValue")) {
			tfRestingPotential.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getUpperValue")) {
			tfResistance.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "getFiringProbability")) {
			tfTimeStep.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, IntegrateAndFireNeuron.class, "isAddNoise")) {
		    isAddNoise.setNull();
		}
		
		randTab.fillFieldValues(getRandomizers());
        
    }
    
    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((LinearNeuron)neuron_list.get(i)).getAddNoise());
		}
		return ret;
    }
    
    public void fillDefaultValues(){
        IntegrateAndFireNeuron neuronRef = new IntegrateAndFireNeuron();
		tfRestingPotential.setText(Double.toString(neuronRef.getResistance()));
		tfResistance.setText(Double.toString(neuronRef.getRestingPotential()));
		tfTimeStep.setText(Double.toString(neuronRef.getTimeStep()));
        isAddNoise.setSelected(neuronRef.isAddNoise());
        randTab.fillDefaultValues();
    }
    
    public void commitChanges(){
        
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
            if (isAddNoise.isNull() == false) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }
            randTab.commitRandom(neuronRef.getAddNoise());
        }
    }
}

