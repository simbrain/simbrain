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
import org.simbrain.network.synapses.HebbianThresholdSynapse;
import org.simbrain.util.TristateDropDown;


/**
 * <b>HebbianThresholdSynapsePanel</b>.
 */
public class HebbianThresholdSynapsePanel extends AbstractSynapsePanel {

    /** Learning rate field. */
    private JTextField tfLearningRate = new JTextField();

    /** Output threshold momentum field. */
    private JTextField tfOutputThresholdMomentum = new JTextField();

    /** Output threshold. */
    private JTextField tfOutputThreshold = new JTextField();

    /** Output threshold combo box. */
    private TristateDropDown isOutputThreshold = new TristateDropDown();

    /** Synapse refernece. */
    private HebbianThresholdSynapse synapseRef;

    /**
     * This method is the default constructor.
     */
    public HebbianThresholdSynapsePanel() {
        this.addItem("Learning rate", tfLearningRate);
        this.addItem("Output threshold momentum", tfOutputThresholdMomentum);
        this.addItem("Output threshold", tfOutputThreshold);
        this.addItem("Use sliding output threshold", isOutputThreshold);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (HebbianThresholdSynapse) ruleList.get(0);

        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));
        tfOutputThresholdMomentum.setText(Double.toString(synapseRef.getOutputThresholdMomentum()));
        tfOutputThreshold.setText(Double.toString(synapseRef.getOutputThreshold()));
        isOutputThreshold.setSelected(synapseRef.getUseSlidingOutputThreshold());

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdSynapse.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdSynapse.class, "getOutputThresholdMomentum")) {
            tfOutputThresholdMomentum.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdSynapse.class, "getOutputThreshold")) {
            tfOutputThreshold.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdSynapse.class, "getUseSlidingOutputThreshold")) {
            isOutputThreshold.setNull();
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
//        HebbianThresholdSynapse synapseRef = new HebbianThresholdSynapse();
        tfLearningRate.setText(Double.toString(HebbianThresholdSynapse.DEFAULT_LEARNING_RATE));
        tfOutputThresholdMomentum.setText(Double.toString(HebbianThresholdSynapse.DEFAULT_OUTPUT_THRESHOLD_MOMENTUM));
        tfOutputThreshold.setText(Double.toString(HebbianThresholdSynapse.DEFAULT_OUTPUT_THRESHOLD));
        isOutputThreshold.setSelected(HebbianThresholdSynapse.DEFAULT_USE_SLIDING_OUTPUT_THRESHOLD);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            HebbianThresholdSynapse synapseRef = (HebbianThresholdSynapse) ruleList.get(i);

            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                synapseRef.setLearningRate(Double.parseDouble(tfLearningRate.getText()));
            }

            if (!tfOutputThresholdMomentum.getText().equals(NULL_STRING)) {
                synapseRef.setOutputThresholdMomentum(Double.parseDouble(tfOutputThresholdMomentum.getText()));
            }

            if (!tfOutputThreshold.getText().equals(NULL_STRING)) {
                synapseRef.setOutputThreshold(Double.parseDouble(tfOutputThreshold.getText()));
            }

            if (!isOutputThreshold.isNull()) {
                synapseRef.setUseSlidingOutputThreshold(isOutputThreshold.isSelected());
            }
        }
    }
}
