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

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.synapses.TraceSynapse;

/**
 * <b>ClampedSynapsePanel</b>.
 */
public class TraceSynapsePanel extends AbstractSynapsePanel {

    /** Momentum field. */
    private JTextField tfMomentum = new JTextField();

    /**
     * This method is the default constructor.
     */
    public TraceSynapsePanel() {
        addItem("Learning rate", tfMomentum);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        TraceSynapse synapseRef = (TraceSynapse) synapse_list.get(0);

        tfMomentum.setText(Double.toString(synapseRef.getMomentum()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(synapse_list, TraceSynapse.class, "getMomentum")) {
            tfMomentum.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        TraceSynapse synapseRef = new TraceSynapse();

        tfMomentum.setText(Double.toString(synapseRef.getMomentum()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < synapse_list.size(); i++) {
            TraceSynapse synapseRef = (TraceSynapse) synapse_list.get(i);

            if (!tfMomentum.getText().equals(NULL_STRING)) {
                synapseRef.setMomentum(Double.parseDouble(tfMomentum.getText()));
            }
        }
    }
}
