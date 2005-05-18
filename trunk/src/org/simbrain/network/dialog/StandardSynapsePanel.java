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

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.interfaces.LearningRule;
import org.simnet.interfaces.*;
import org.simnet.synapses.StandardSynapse;


/**
 * 
 */

public class StandardSynapsePanel extends AbstractSynapsePanel {
	
	private JTextField tfStrength = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JComboBox cbLearningRule = new JComboBox(LearningRule.getList());
		
	public StandardSynapsePanel(){
		addItem("Strength", tfStrength);
		addItem("Learning rule", cbLearningRule);
		addItem("Upper bound", tfUpBound);
		addItem("Lower bound", tfLowBound);
		addItem("Increment", tfIncrement);	
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		Synapse synapse_ref = (Synapse)synapse_list.get(0);
		
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
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
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

		}


   }

}
