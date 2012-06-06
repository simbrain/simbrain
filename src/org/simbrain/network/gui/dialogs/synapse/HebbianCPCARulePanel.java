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
import org.simbrain.network.synapse_update_rules.HebbianCPCARule;

/**
 * <b>HebbianCPCAPanel</b>.
 */
public class HebbianCPCARulePanel extends AbstractSynapsePanel {
    private static final long serialVersionUID = 1L;

    /** Learning rate field. */
    private JTextField tfLearningRate = new JTextField();

    /** Maximum weight value (see equation 4.19 in O'Reilly and Munakata). */
    private JTextField tfM = new JTextField();

    /** Weight offset. */
    private JTextField tfTheta = new JTextField();

    /** Sigmoidal function. */
    private JTextField tfLambda = new JTextField();

    /** Synapse reference. */
    private HebbianCPCARule synapseRef;

    /**
     * This method is the default constructor.
     */
    public HebbianCPCARulePanel() {
        this.addItem("Learning rate", tfLearningRate);
        this.addItem("Maximum Weight Value", tfM);
        this.addItem("Weight Offset Value", tfTheta);
        this.addItem("Sigmoidal Function", tfLambda);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (HebbianCPCARule) ruleList.get(0);

        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));
        tfM.setText(Double.toString(synapseRef.getM()));
        tfTheta.setText(Double.toString(synapseRef.getTheta()));
        tfLambda.setText(Double.toString(synapseRef.getLambda()));

        // Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class, "getM")) {
            tfM.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getTheta")) {
            tfTheta.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getLambda")) {
            tfLambda.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        // HebbianCPCA synapseRef = new HebbianCPCA();
        tfLearningRate.setText(Double
                .toString(HebbianCPCARule.DEFAULT_LEARNING_RATE));
        tfM.setText(Double.toString(HebbianCPCARule.DEFAULT_M));
        tfTheta.setText(Double.toString(HebbianCPCARule.DEFAULT_THETA));
        tfLambda.setText(Double.toString(HebbianCPCARule.DEFAULT_LAMBDA));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            HebbianCPCARule synapseRef = (HebbianCPCARule) ruleList.get(i);

            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                synapseRef.setLearningRate(Double.parseDouble(tfLearningRate
                        .getText()));
            }
            if (!tfM.getText().equals(NULL_STRING)) {
                synapseRef.setM(Double.parseDouble(tfM.getText()));
            }
            if (!tfTheta.getText().equals(NULL_STRING)) {
                synapseRef.setTheta(Double.parseDouble(tfTheta.getText()));
            }
            if (!tfLambda.getText().equals(NULL_STRING)) {
                synapseRef.setLambda(Double.parseDouble(tfLambda.getText()));
            }
        }
    }
}
