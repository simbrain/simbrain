/*
 * Part of HDV (High-Dimensional-Visualizer), a tool for visualizing high
 * dimensional datasets.
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.awt.*;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.StandardNeuron;


/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class NeuronDialog extends StandardDialog {

	private LabelledItemPanel mainPanel = new LabelledItemPanel();
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfDecay = new JTextField();
	private JTextField tfBias = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JComboBox cbActivationRule = new JComboBox(ActivationRule.getList());

	private static final String NULL_STRING = "...";
	private ArrayList neuron_list; // The neurons being modified
	private Neuron neuron_ref;
	
	/**
	  * This method is the default constructor.
	  */
	 public NeuronDialog(ArrayList selectedNeurons) 
	 {
	 	neuron_list = selectedNeurons;
		init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
	 	//Initialize Dialog
		setTitle("Neuron Dialog");
		fillFieldValues();
		this.setLocation(500, 0); //Sets location of network dialog

		
		//Set up grapics panel
		mainPanel.addItem("Activation", tfActivation);
		mainPanel.addItem("Activation Function", cbActivationRule);
		mainPanel.addItem("Upper bound", tfUpBound);
		mainPanel.addItem("Lower bound", tfLowBound);
		mainPanel.addItem("Increment", tfIncrement);
		mainPanel.addItem("Bias", tfBias);
		mainPanel.addItem("Decay", tfDecay);		

		setContentPane(mainPanel);

	 }
		
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		neuron_ref = (Neuron)neuron_list.get(0);
		
		tfActivation.setText(Double.toString(neuron_ref.getActivation()));
		cbActivationRule.setSelectedIndex(ActivationRule.getActivationFunctionIndex(neuron_ref.getActivationFunction().getName()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));
		tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		tfDecay.setText(Double.toString(neuron_ref.getDecay()));

		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getActivation")) {
			tfActivation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getActivationFunctionS")) {
			cbActivationRule.addItem(NULL_STRING);
			cbActivationRule.setSelectedIndex(ActivationRule.getList().length);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getDecay")) {
			tfDecay.setText(NULL_STRING);
		}	

		
	}
	 
	/**
	* Set projector values based on fields 
	*/
   public void getValues() {
   	
	for (int i = 0; i < neuron_list.size(); i++) {
		Neuron neuron_ref = (Neuron) neuron_list.get(i);

		if (tfActivation.getText().equals(NULL_STRING) == false) {
			neuron_ref.setActivation(
				Double.parseDouble(tfActivation.getText()));
		}
		if (cbActivationRule.getSelectedItem().equals(NULL_STRING)== false) {
			neuron_ref.setActivationFunction(ActivationRule.getActivationFunction(cbActivationRule.getSelectedItem().toString()));
		}
		if (tfUpBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setUpperBound(
				Double.parseDouble(tfUpBound.getText()));
		}
		if (tfLowBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setLowerBound(
				Double.parseDouble(tfLowBound.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			neuron_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}
		if (tfDecay.getText().equals(NULL_STRING) == false) {
			neuron_ref.setDecay(
				Double.parseDouble(tfDecay.getText()));
		}
		if (tfBias.getText().equals(NULL_STRING) == false) {
			neuron_ref.setBias(Double.parseDouble(tfBias.getText()));
		}
	   	
	}

   }
  

}
