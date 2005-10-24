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

import org.simnet.synapses.spikeresponders.Step;

import javax.swing.JTextField;


/**
 * <b>StepSpikerPanel</b>
 */
public class StepSpikerPanel extends AbstractSpikeResponsePanel {
    private JTextField tfResponseHeight = new JTextField();
    private JTextField tfResponseTime = new JTextField();

    public StepSpikerPanel() {
        tfResponseHeight.setColumns(6);
        this.addItem("Response height", tfResponseHeight);
        this.addItem("Response time", tfResponseTime);
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        Step spikeResponder = (Step) spikeResponderList.get(0);

        tfResponseHeight.setText(Double.toString(spikeResponder.getResponseHeight()));
        tfResponseTime.setText(Double.toString(spikeResponder.getResponseTime()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(spikeResponderList, Step.class, "getResponseHeight")) {
            tfResponseHeight.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(spikeResponderList, Step.class, "getResponseTime")) {
            tfResponseTime.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type
     */
    public void fillDefaultValues() {
        Step spiker_ref = new Step();
        tfResponseHeight.setText(Double.toString(spiker_ref.getResponseHeight()));
        tfResponseTime.setText(Double.toString(spiker_ref.getResponseTime()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
        for (int i = 0; i < spikeResponderList.size(); i++) {
            Step step_ref = (Step) spikeResponderList.get(i);

            if (tfResponseHeight.getText().equals(NULL_STRING) == false) {
                step_ref.setResponseHeight(Double.parseDouble(tfResponseHeight.getText()));
            }

            if (tfResponseTime.getText().equals(NULL_STRING) == false) {
                step_ref.setResponseTime(Double.parseDouble(tfResponseTime.getText()));
            }
        }
    }
}
