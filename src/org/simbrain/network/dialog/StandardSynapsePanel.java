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
	
	private JComboBox cbLearningRule = new JComboBox(LearningRule.getList());
		
	public StandardSynapsePanel(){
		addItem("Learning rule", cbLearningRule);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		Synapse synapse_ref = (Synapse)synapse_list.get(0);
		
		cbLearningRule.setSelectedIndex(LearningRule.getLearningRuleIndex(synapse_ref.getLearningRule().getName()));

		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(synapse_list, StandardSynapse.class, "getLearningRuleS")) {
			cbLearningRule.addItem(NULL_STRING);
			cbLearningRule.setSelectedIndex(LearningRule.getList().length);
		}	

	}
	
	/**
	 * Fill field values to default values for this synapse type
	 */
	public void fillDefaultValues() {
		StandardSynapse synapse_ref = new StandardSynapse();
		cbLearningRule.setSelectedIndex(LearningRule.getLearningRuleIndex(synapse_ref.getLearningRule().getName()));
	}
	

    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
		for (int i = 0; i < synapse_list.size(); i++) {
			StandardSynapse synapse_ref = (StandardSynapse) synapse_list.get(i);

			if (cbLearningRule.getSelectedItem().equals(NULL_STRING)== false) {
				synapse_ref.setLearningRule(LearningRule.getLearningRule(cbLearningRule.getSelectedItem().toString()));
			}

		}


   }

}
