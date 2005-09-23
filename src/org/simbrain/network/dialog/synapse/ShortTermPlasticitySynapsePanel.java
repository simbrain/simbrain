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
package org.simbrain.network.dialog.synapse;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.TristateDropDown;
import org.simnet.synapses.ShortTermPlasticitySynapse;

/**
 * 
 * <b>ShortTermPlasticitySynapsePanel</b>
 */
public class ShortTermPlasticitySynapsePanel extends AbstractSynapsePanel {
	
	private JTextField tfBaseLineStrength = new JTextField();
	private JTextField tfTimeConstant = new JTextField();
	private JTextField tfBumpRate = new JTextField();
	private JTextField tfDecayRate = new JTextField();
    private TristateDropDown cbPlasticityType = new TristateDropDown("Depression", "Facilitation");
	
	private ShortTermPlasticitySynapse synapse_ref;
	
	public ShortTermPlasticitySynapsePanel(){
        this.addItem("Plasticity type", cbPlasticityType);
		this.addItem("Base-line-strength", tfBaseLineStrength);
		this.addItem("Time-constant", tfTimeConstant);
		this.addItem("Growth-rate", tfBumpRate);
		this.addItem("Decay-rate", tfDecayRate);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		synapse_ref = (ShortTermPlasticitySynapse)synapse_list.get(0);
		
        cbPlasticityType.setSelectedIndex(synapse_ref.getPlasticityType());
		tfBaseLineStrength.setText(Double.toString(synapse_ref.getBaseLineStrength()));
		tfTimeConstant.setText(Double.toString(synapse_ref.getTimeConstant()));
		tfBumpRate.setText(Double.toString(synapse_ref.getBumpRate()));
		tfDecayRate.setText(Double.toString(synapse_ref.getDecayRate()));

		//Handle consistency of multiply selections
        if(!NetworkUtils.isConsistent(synapse_list, ShortTermPlasticitySynapse.class, "getPlasticityType")) {
            cbPlasticityType.setNull();
        }
		if(!NetworkUtils.isConsistent(synapse_list, ShortTermPlasticitySynapse.class, "getBaseLineStrength")) {
			tfBaseLineStrength.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, ShortTermPlasticitySynapse.class, "getTimeConstant")) {
			tfTimeConstant.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, ShortTermPlasticitySynapse.class, "getBumpRate")) {
			tfBumpRate.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, ShortTermPlasticitySynapse.class, "getDecayRate")) {
			tfDecayRate.setText(NULL_STRING);
		}
	}
	
	/**
	 * Fill field values to default values for this synapse type
	 */
	public void fillDefaultValues() {
	    ShortTermPlasticitySynapse synapse_ref = new ShortTermPlasticitySynapse();
        cbPlasticityType.setSelectedIndex(synapse_ref.getPlasticityType());
		tfBaseLineStrength.setText(Double.toString(synapse_ref.getBaseLineStrength()));
		tfTimeConstant.setText(Double.toString(synapse_ref.getTimeConstant()));
		tfBumpRate.setText(Double.toString(synapse_ref.getBumpRate()));
		tfDecayRate.setText(Double.toString(synapse_ref.getDecayRate()));
	}

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < synapse_list.size(); i++) {
            ShortTermPlasticitySynapse synapse_ref = (ShortTermPlasticitySynapse) synapse_list.get(i);

            if (cbPlasticityType.isNull() == false) {
                synapse_ref.setPlasticityType(cbPlasticityType.getSelectedIndex());
            }
            if (tfBaseLineStrength.getText().equals(NULL_STRING) == false) {
                synapse_ref.setBaseLineStrength(Double
                        .parseDouble(tfBaseLineStrength.getText()));
            }
            if (tfTimeConstant.getText().equals(NULL_STRING) == false) {
                synapse_ref.setTimeConstant(Double
                        .parseDouble(tfTimeConstant.getText()));
            }
            if (tfBumpRate.getText().equals(NULL_STRING) == false) {
                synapse_ref.setBumpRate(Double
                        .parseDouble(tfBumpRate.getText()));
            }
            if (tfDecayRate.getText().equals(NULL_STRING) == false) {
                synapse_ref.setDecayRate(Double
                        .parseDouble(tfDecayRate.getText()));
            }
        }
    }
}
