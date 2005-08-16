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

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.TristateDropDown;
import org.simnet.synapses.Hebbian;


/**
 * 
 */

public class HebbianSynapsePanel extends AbstractSynapsePanel {
	
	private JTextField tfMomentum = new JTextField();
	private JTextField tfInputThreshold = new JTextField();
	private TristateDropDown isInputThreshold = new TristateDropDown();
	private JTextField tfOutputThreshold = new JTextField();
	private TristateDropDown isOutputThreshold = new TristateDropDown();
	
	
	private Hebbian synapse_ref;
	
	public HebbianSynapsePanel(){
		this.addItem("Momentum", tfMomentum);
		this.addItem("Input threshold", tfInputThreshold);
		this.addItem("Use sliding input threshold", isInputThreshold);
		this.addItem("Output threshold", tfOutputThreshold);
		this.addItem("Uset sliding output threshold", isOutputThreshold);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		synapse_ref = (Hebbian)synapse_list.get(0);
		
		tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
		tfInputThreshold.setText(Double.toString(synapse_ref.getInputThreshold()));
		tfOutputThreshold.setText(Double.toString(synapse_ref.getOutputThreshold()));
		isInputThreshold.setSelected(synapse_ref.isUseSlidingInputThreshold());
		isOutputThreshold.setSelected(synapse_ref.isUseSlidingOutputThreshold());
		
		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "getMomentum")) {
			tfMomentum.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "getInputThreshold")) {
			tfInputThreshold.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "isUseSlidingInputThreshold")) {
			isInputThreshold.setNull();
		}
		if(!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "getOutputThreshold")) {
			tfOutputThreshold.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "isUseSlidingOutputThreshold")) {
			isOutputThreshold.setNull();
		}

	}
	
	/**
	 * Fill field values to default values for this synapse type
	 */
	public void fillDefaultValues() {
		Hebbian synapse_ref = new Hebbian();
		tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
		tfInputThreshold.setText(Double.toString(synapse_ref.getInputThreshold()));
		isInputThreshold.setSelected(synapse_ref.isUseSlidingInputThreshold());
		tfOutputThreshold.setText(Double.toString(synapse_ref.getOutputThreshold()));
		isOutputThreshold.setSelected(synapse_ref.isUseSlidingOutputThreshold());
	}

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < synapse_list.size(); i++) {
            Hebbian synapse_ref = (Hebbian) synapse_list.get(i);

            if (tfMomentum.getText().equals(NULL_STRING) == false) {
                synapse_ref.setMomentum(Double
                        .parseDouble(tfMomentum.getText()));
            }
            if (tfInputThreshold.getText().equals(NULL_STRING) == false){
                synapse_ref.setInputThreshold(Double
                        .parseDouble(tfInputThreshold.getText()));
            }
            if (isInputThreshold.isNull() == false){
                synapse_ref.setUseSlidingInputThreshold(isInputThreshold.isSelected());
            }
            if (tfOutputThreshold.getText().equals(NULL_STRING) == false){
                synapse_ref.setOutputThreshold(Double
                        .parseDouble(tfOutputThreshold.getText()));
            }
            if (isOutputThreshold.isNull() == false){
                synapse_ref.setUseSlidingOutputThreshold(isOutputThreshold.isSelected());
            }
        }
    }
}
