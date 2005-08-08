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
import org.simnet.neurons.PiecewiseLinearNeuron;

public class PiecewiseLinearNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfSlope = new JTextField();
    private JTextField tfBias = new JTextField();
    private JTextField tfLowValue = new JTextField();
    private JTextField tfUpValue = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel mainTab = new LabelledItemPanel();
	private RandomPanel randTab = new RandomPanel();
	private TristateDropDown isAddNoise = new TristateDropDown();
    
    public PiecewiseLinearNeuronPanel(){
        
        this.add(tabbedPane);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Lower bound", tfLowValue);
        mainTab.addItem("Upper bound", tfUpValue);
		mainTab.addItem("Add Noise", isAddNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
    }
    
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		PiecewiseLinearNeuron neuron_ref = (PiecewiseLinearNeuron)neuron_list.get(0);
		
		tfSlope.setText(Double.toString(neuron_ref.getSlope()));
		tfLowValue.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpValue.setText(Double.toString(neuron_ref.getUpperBound()));
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		isAddNoise.setSelected(neuron_ref.isAddNoise());

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, PiecewiseLinearNeuron.class, "getSlope")) {
			tfSlope.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, PiecewiseLinearNeuron.class, "getLowerBound")) {
			tfLowValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, PiecewiseLinearNeuron.class, "getUpperBound")) {
			tfUpValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, PiecewiseLinearNeuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, PiecewiseLinearNeuron.class, "isAddNoise")){
		    isAddNoise.setNull();
		}
		randTab.fillFieldValues(getRandomizers());
	}
	
    private ArrayList getRandomizers() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < neuron_list.size(); i++) {
			ret.add(((PiecewiseLinearNeuron)neuron_list.get(i)).getNoise());
		}
		return ret;
    }
	
	/**
	 * Fill field values to default values for binary neuron
	 *
	 */
	public void fillDefaultValues() {
		PiecewiseLinearNeuron neuron_ref = new PiecewiseLinearNeuron();
		tfSlope.setText(Double.toString(neuron_ref.getSlope()));
		tfLowValue.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpValue.setText(Double.toString(neuron_ref.getUpperBound()));
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		isAddNoise.setSelected(neuron_ref.isAddNoise());
		randTab.fillDefaultValues();

	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            PiecewiseLinearNeuron neuron_ref = (PiecewiseLinearNeuron) neuron_list.get(i);

            if (tfSlope.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSlope(Double.parseDouble(tfSlope
                        .getText()));
            }
            if (tfUpValue.getText().equals(NULL_STRING) == false) {
                neuron_ref.setUpperBound(Double
                        .parseDouble(tfUpValue.getText()));
            }
            if (tfLowValue.getText().equals(NULL_STRING) == false) {
                neuron_ref.setLowerBound(Double.parseDouble(tfLowValue
                        .getText()));
            }
            if (tfBias.getText().equals(NULL_STRING) == false) {
                neuron_ref.setBias(Double.parseDouble(tfBias
                        .getText()));
            }
            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoise());
        }
    }
}
