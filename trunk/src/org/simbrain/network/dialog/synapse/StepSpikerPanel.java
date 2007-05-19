/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
import org.simnet.synapses.spikeresponders.Step;


/**
 * <b>StepSpikerPanel</b>.
 */
public class StepSpikerPanel extends AbstractSpikeResponsePanel {

    /** Response height field. */
    private JTextField tfResponseHeight = new JTextField();

    /** Response time field. */
    private JTextField tfResponseTime = new JTextField();

    /**
     * This method is the default constructor.
     *
     */
    public StepSpikerPanel() {
        tfResponseHeight.setColumns(6);
        this.addItem("Response height", tfResponseHeight);
        this.addItem("Response time", tfResponseTime);
    }

    /**
     * Populate fields with current data.
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
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        Step spikerRef = new Step();
        tfResponseHeight.setText(Double.toString(spikerRef.getResponseHeight()));
        tfResponseTime.setText(Double.toString(spikerRef.getResponseTime()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < spikeResponderList.size(); i++) {
            Step stepRef = (Step) spikeResponderList.get(i);

            if (!tfResponseHeight.getText().equals(NULL_STRING)) {
                stepRef.setResponseHeight(Double.parseDouble(tfResponseHeight.getText()));
            }

            if (!tfResponseTime.getText().equals(NULL_STRING)) {
                stepRef.setResponseTime(Double.parseDouble(tfResponseTime.getText()));
            }
        }
    }
}
