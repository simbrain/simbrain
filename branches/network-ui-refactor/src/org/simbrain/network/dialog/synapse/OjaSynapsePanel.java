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
import org.simnet.synapses.OjaSynapse;


/**
 * <b>OjaSynapsePanel</b>
 */
public class OjaSynapsePanel extends AbstractSynapsePanel {
    private JTextField tfMomentum = new JTextField();
    private JTextField tfAlpha = new JTextField();
    private OjaSynapse synapse_ref;

    public OjaSynapsePanel() {
        addItem("Momentum", tfMomentum);
        addItem("Alpha", tfAlpha);
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        synapse_ref = (OjaSynapse) synapse_list.get(0);

        tfAlpha.setText(Double.toString(synapse_ref.getAlpha()));
        tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(synapse_list, OjaSynapse.class, "getAlpha")) {
            tfAlpha.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapse_list, OjaSynapse.class, "getMomentum")) {
            tfMomentum.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type
     */
    public void fillDefaultValues() {
        OjaSynapse synapse_ref = new OjaSynapse();
        tfAlpha.setText(Double.toString(synapse_ref.getAlpha()));
        tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
        for (int i = 0; i < synapse_list.size(); i++) {
            OjaSynapse synapse_ref = (OjaSynapse) synapse_list.get(i);

            if (tfAlpha.getText().equals(NULL_STRING) == false) {
                synapse_ref.setAlpha(Double.parseDouble(tfAlpha.getText()));
            }

            if (tfMomentum.getText().equals(NULL_STRING) == false) {
                synapse_ref.setMomentum(Double.parseDouble(tfMomentum.getText()));
            }
        }
    }
}
