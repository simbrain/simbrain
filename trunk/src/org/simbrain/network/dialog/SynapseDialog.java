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
import org.simnet.interfaces.*;
import org.simnet.synapses.*;


/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class SynapseDialog extends StandardDialog {

	private LabelledItemPanel mainPanel = new LabelledItemPanel();
	
	private JTextField tfStrength = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JComboBox cbLearningRule = new JComboBox(LearningRule.getList());

	private JTextField tfMomentum = new JTextField();

	
	private static final String NULL_STRING = "...";
	private ArrayList synapse_list; // The neurons being modified
	private StandardSynapse synapse_ref;
	
	/**
	  * This method is the default constructor.
	  */
	 public SynapseDialog(ArrayList selectedSynapses) 
	 {
	 	synapse_list = selectedSynapses;
		init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
	 	//Initialize Dialog
		setTitle("Synapse Dialog");
		fillFieldValues();
		this.setLocation(500, 0); //Sets location of network dialog

		
		//Set up grapics panel
		mainPanel.addItem("Strength", tfStrength);
		mainPanel.addItem("Learning rule", cbLearningRule);
		mainPanel.addItem("Upper bound", tfUpBound);
		mainPanel.addItem("Lower bound", tfLowBound);
		mainPanel.addItem("Increment", tfIncrement);
		
		if (synapse_list.get(0) instanceof Hebbian) {
			mainPanel.addItem("Momentum", tfMomentum);
		}

		setContentPane(mainPanel);

	 }
		
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		synapse_ref = (StandardSynapse)synapse_list.get(0);
		
		tfStrength.setText(Double.toString(synapse_ref.getStrength()));
		cbLearningRule.setSelectedIndex(LearningRule.getLearningRuleIndex(synapse_ref.getLearningRule().getName()));
		tfLowBound.setText(Double.toString(synapse_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(synapse_ref.getUpperBound()));
		tfIncrement.setText(Double.toString(synapse_ref.getIncrement()));

		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(synapse_list, StandardSynapse.class, "getStrength")) {
			tfStrength.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, StandardSynapse.class, "getLearningRuleS")) {
			cbLearningRule.addItem(NULL_STRING);
			cbLearningRule.setSelectedIndex(LearningRule.getList().length);
		}	
		if(!NetworkUtils.isConsistent(synapse_list, StandardSynapse.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(synapse_list, StandardSynapse.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(synapse_list, StandardSynapse.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}	
		
	}
	 
	/**
	* Set projector values based on fields 
	*/
   public void getValues() {
   	
	for (int i = 0; i < synapse_list.size(); i++) {
		StandardSynapse synapse_ref = (StandardSynapse) synapse_list.get(i);

		if (tfStrength.getText().equals(NULL_STRING) == false) {
			synapse_ref.setStrength(
				Double.parseDouble(tfStrength.getText()));
		}
		if (cbLearningRule.getSelectedItem().equals(NULL_STRING)== false) {
			synapse_ref.setLearningRule(LearningRule.getLearningRule(cbLearningRule.getSelectedItem().toString()));
		}
		if (tfUpBound.getText().equals(NULL_STRING) == false) {
			synapse_ref.setUpperBound(
				Double.parseDouble(tfUpBound.getText()));
		}
		if (tfLowBound.getText().equals(NULL_STRING) == false) {
			synapse_ref.setLowerBound(
				Double.parseDouble(tfLowBound.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			synapse_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}

		if (synapse_list.get(0) instanceof Hebbian) {
			((Hebbian)synapse_list.get(0)).setMomentum(Double.parseDouble(tfMomentum.getText()));
		}

	}

   }
  

}
