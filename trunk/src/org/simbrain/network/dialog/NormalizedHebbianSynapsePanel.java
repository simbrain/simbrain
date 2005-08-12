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
import org.simnet.synapses.NormalizedHebbianSynapse;

/**
 * @author Kyle Baron
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NormalizedHebbianSynapsePanel extends AbstractSynapsePanel {
	private JTextField tfMomentum = new JTextField();
	private JTextField tfOverallWeight = new JTextField();
	private TristateDropDown cbSummation = new TristateDropDown("Regular", "Sum-of-squares");
	
	private NormalizedHebbianSynapse synapse_ref;
	
	public NormalizedHebbianSynapsePanel(){
	    
		this.addItem("Momentum", tfMomentum);
		this.addItem("Overall weight", tfOverallWeight);
		this.addItem("Summation", cbSummation);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		synapse_ref = (NormalizedHebbianSynapse)synapse_list.get(0);
		
		tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
		tfOverallWeight.setText(Double.toString(synapse_ref.getOverallWeight()));
		cbSummation.setSelectedIndex(synapse_ref.getSummation());

		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(synapse_list, NormalizedHebbianSynapse.class, "getMomentum")) {
			tfMomentum.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, NormalizedHebbianSynapse.class, "getOverallWeight")) {
			tfOverallWeight.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, NormalizedHebbianSynapse.class, "getSummation")) {
			cbSummation.setNull();
		}
	}
	
	/**
	 * Fill field values to default values for this synapse type
	 */
	public void fillDefaultValues() {
	    NormalizedHebbianSynapse synapse_ref = new NormalizedHebbianSynapse();
		tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
		tfOverallWeight.setText(Double.toString(synapse_ref.getOverallWeight()));
		cbSummation.setSelectedIndex(synapse_ref.getSummation());
	}

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < synapse_list.size(); i++) {
            NormalizedHebbianSynapse synapse_ref = (NormalizedHebbianSynapse) synapse_list
                    .get(i);

            if (tfMomentum.getText().equals(NULL_STRING) == false) {
                synapse_ref.setMomentum(Double
                        .parseDouble(tfMomentum.getText()));
            }
            if (tfOverallWeight.getText().equals(NULL_STRING) == false){
                synapse_ref.setOverallWeight(Double.parseDouble(tfOverallWeight.getText()));
            }
            if (cbSummation.isNull() == false){
                synapse_ref.setSummation(cbSummation.getSelectedIndex());
            }
        }
    }
}
