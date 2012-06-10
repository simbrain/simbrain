/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.gui.dialogs.synapse;

import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.synapse_update_rules.spikeresponders.ProbabilisticResponder;

/**
 * <b>ProbabilisticSpikeResponderPanel</b>.
 */
public class ProbabilisticSpikeResponderPanel extends
        AbstractSpikeResponsePanel {

    /** Activation Probability. */
    private JTextField tfActivationProbability = new JTextField();

    /** Response value. */
    private JTextField tfResponseValue = new JTextField();

    /**
     * This method is the default constructor.
     *
     */
    public ProbabilisticSpikeResponderPanel() {
        tfActivationProbability.setColumns(6);
        this.addItem("Activation Probability", tfActivationProbability);
        this.addItem("Response value", tfResponseValue);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        ProbabilisticResponder spikeResponder = (ProbabilisticResponder) spikeResponderList
                .get(0);

        tfActivationProbability.setText(Double.toString(spikeResponder
                .getActivationProbability()));
        tfResponseValue.setText(Double.toString(spikeResponder
                .getResponseValue()));

        // Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(spikeResponderList,
                ProbabilisticResponder.class, "getActivationProbability")) {
            tfActivationProbability.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(spikeResponderList,
                ProbabilisticResponder.class, "getResponseValue")) {
            tfResponseValue.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        ProbabilisticResponder spikerRef = new ProbabilisticResponder();
        tfActivationProbability.setText(Double.toString(spikerRef
                .getActivationProbability()));
        tfResponseValue.setText(Double.toString(spikerRef
                .getActivationProbability()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < spikeResponderList.size(); i++) {
            ProbabilisticResponder stepRef = (ProbabilisticResponder) spikeResponderList
                    .get(i);

            if (!tfActivationProbability.getText().equals(NULL_STRING)) {
                stepRef.setActivationProbability(Double
                        .parseDouble(tfActivationProbability.getText()));
            }

            if (!tfResponseValue.getText().equals(NULL_STRING)) {
                stepRef.setResponseValue(Double.parseDouble(tfResponseValue
                        .getText()));
            }
        }
    }
}
