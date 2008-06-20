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
import org.simbrain.network.synapses.OjaSynapse;


/**
 * <b>OjaSynapsePanel</b>.
 */
public class OjaSynapsePanel extends AbstractSynapsePanel {

    /** Learning rate field. */
    private JTextField tfLearningRate = new JTextField();

    /** Normalize field. */
    private JTextField tfNormalize = new JTextField();

    /** Synapse reference. */
    private OjaSynapse synapseRef;

    /**
     * This method is the default constructor.
     */
    public OjaSynapsePanel() {
        addItem("Learning rate", tfLearningRate);
        addItem("Normalize to", tfNormalize);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (OjaSynapse) synapseList.get(0);

        tfNormalize.setText(Double.toString(synapseRef.getNormalizationFactor()));
        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(synapseList, OjaSynapse.class, "getNormalizationFactor")) {
            tfNormalize.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, OjaSynapse.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
//        OjaSynapse synapseRef = new OjaSynapse();
        tfNormalize.setText(Double.toString(OjaSynapse.DEFAULT_NORMALIZATION_FACTOR));
        tfLearningRate.setText(Double.toString(OjaSynapse.DEFAULT_LEARNING_RATE));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < synapseList.size(); i++) {
            OjaSynapse synapseRef = (OjaSynapse) synapseList.get(i);

            if (!tfNormalize.getText().equals(NULL_STRING)) {
                synapseRef.setNormalizationFactor(Double.parseDouble(tfNormalize.getText()));
            }

            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                synapseRef.setLearningRate(Double.parseDouble(tfLearningRate.getText()));
            }
        }
    }
}
