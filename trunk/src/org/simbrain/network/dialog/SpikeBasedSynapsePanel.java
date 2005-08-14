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
import org.simnet.synapses.SpikeBasedSynapse;


public class SpikeBasedSynapsePanel extends AbstractSynapsePanel{
	
	private JTextField tfTimeConstant = new JTextField();
	private JTextField tfOpenRate = new JTextField();
	private JTextField tfCloseRate = new JTextField();
	
	private SpikeBasedSynapse synapse_ref;
	
	public SpikeBasedSynapsePanel(){
		this.addItem("Time constant", tfTimeConstant);
		this.addItem("Open rate", tfOpenRate);
		this.addItem("Close rate", tfCloseRate);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		synapse_ref = (SpikeBasedSynapse)synapse_list.get(0);
		
		tfTimeConstant.setText(Double.toString(synapse_ref.getTimeConstant()));
		tfOpenRate.setText(Double.toString(synapse_ref.getOpenRate()));
		tfCloseRate.setText(Double.toString(synapse_ref.getCloseRate()));

		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(synapse_list, SpikeBasedSynapse.class, "getMomentum")) {
			tfTimeConstant.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, SpikeBasedSynapse.class, "getOpenRate")) {
			tfOpenRate.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(synapse_list, SpikeBasedSynapse.class, "getCloseRate")) {
			tfCloseRate.setText(NULL_STRING);
		}

	}
	
	/**
	 * Fill field values to default values for this synapse type
	 */
	public void fillDefaultValues() {
	    SpikeBasedSynapse synapse_ref = new SpikeBasedSynapse();
		tfTimeConstant.setText(Double.toString(synapse_ref.getTimeConstant()));
		tfOpenRate.setText(Double.toString(synapse_ref.getOpenRate()));
		tfCloseRate.setText(Double.toString(synapse_ref.getCloseRate()));
	}

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < synapse_list.size(); i++) {
            SpikeBasedSynapse synapse_ref = (SpikeBasedSynapse) synapse_list.get(i);

            if (tfTimeConstant.getText().equals(NULL_STRING) == false) {
                synapse_ref.setTimeConstant(Double
                        .parseDouble(tfTimeConstant.getText()));
            }
            if (tfOpenRate.getText().equals(NULL_STRING) == false) {
                synapse_ref.setOpenRate(Double
                        .parseDouble(tfOpenRate.getText()));
            }
            if (tfCloseRate.getText().equals(NULL_STRING) == false) {
                synapse_ref.setCloseRate(Double
                        .parseDouble(tfCloseRate.getText()));
            }
        }
    }
}
