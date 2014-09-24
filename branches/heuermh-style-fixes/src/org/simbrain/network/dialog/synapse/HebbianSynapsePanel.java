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
package org.simbrain.network.dialog.synapse;

import org.simbrain.network.NetworkUtils;

import org.simbrain.util.TristateDropDown;

import org.simnet.synapses.Hebbian;

import javax.swing.JTextField;


/**
 * <b>HebbianSynapsePanel</b>
 */
public class HebbianSynapsePanel extends AbstractSynapsePanel {
    private JTextField tfMomentum = new JTextField();
    private Hebbian synapse_ref;

    public HebbianSynapsePanel() {
        this.addItem("Momentum", tfMomentum);
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        synapse_ref = (Hebbian) synapse_list.get(0);

        tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "getMomentum")) {
            tfMomentum.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type
     */
    public void fillDefaultValues() {
        Hebbian synapse_ref = new Hebbian();
        tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
        for (int i = 0; i < synapse_list.size(); i++) {
            Hebbian synapse_ref = (Hebbian) synapse_list.get(i);

            if (tfMomentum.getText().equals(NULL_STRING) == false) {
                synapse_ref.setMomentum(Double.parseDouble(tfMomentum.getText()));
            }
        }
    }
}