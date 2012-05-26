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
import org.simbrain.network.synapse_update_rules.HebbianRule;


/**
 * <b>HebbianSynapsePanel</b>.
 */
public class HebbianRulePanel extends AbstractSynapsePanel {

    /** Learning rate field. */
    private JTextField tfLearningRate = new JTextField();

    /** Synapse reference. */
    private HebbianRule synapseRef;

    /**
     * This method is the default constructor.
     */
    public HebbianRulePanel() {
        this.addItem("Learning rate", tfLearningRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (HebbianRule) ruleList.get(0);

        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(ruleList, HebbianRule.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        tfLearningRate.setText(Double.toString(HebbianRule.DEFAULT_LEARNING_RATE));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            HebbianRule synapseRef = (HebbianRule) ruleList.get(i);
            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                synapseRef.setLearningRate(Double.parseDouble(tfLearningRate.getText()));
            }
        }
    }
}
