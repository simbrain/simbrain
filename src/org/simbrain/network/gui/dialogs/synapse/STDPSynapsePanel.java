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
import org.simbrain.network.synapses.STDPSynapse;;


/**
 * <b>ShortTermPlasticitySynapsePanel</b>.
 */
public class STDPSynapsePanel extends AbstractSynapsePanel {

    /** Tau minus. */
    private JTextField tfTauMinus = new JTextField();

    /** Tau plus. */
    private JTextField tfTauPLus= new JTextField();

    /** W minus. */
    private JTextField tfWMinus = new JTextField();

    /** WÊplus. */
    private JTextField tfWPlus= new JTextField();

    /** Learning rate. */
    private JTextField tfLearningRate = new JTextField();

    /** Synapse reference. */
    private STDPSynapse synapseRef;

    /**
     * Creates a short term plasticity synapse panel.
     */
    public STDPSynapsePanel() {
        this.addItem("Tau minus", tfTauMinus);
        this.addItem("Tau plus", tfTauPLus);
        this.addItem("W+", tfWPlus);
        this.addItem("W-", tfWMinus);
        this.addItem("Learning rate", tfLearningRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (STDPSynapse) ruleList.get(0);

        tfTauMinus.setText(Double.toString(synapseRef.getTau_minus()));
        tfTauPLus.setText(Double.toString(synapseRef.getTau_plus()));
        tfWMinus.setText(Double.toString(synapseRef.getW_minus()));
        tfWPlus.setText(Double.toString(synapseRef.getW_plus()));
        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));

        // Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(ruleList, STDPSynapse.class, "getTau_minus")) {
            tfTauMinus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPSynapse.class, "getTau_plus")) {
            tfTauPLus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPSynapse.class, "getW_minus")) {
            tfWMinus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPSynapse.class, "getW_plus")) {
            tfWPlus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPSynapse.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        // TODO
////        ShortTermPlasticitySynapse synapseRef = new ShortTermPlasticitySynapse();
//        cbPlasticityType.setSelectedIndex(ShortTermPlasticitySynapse.DEFAULT_PLASTICITY_TYPE);
//        tfBaseLineStrength.setText(Double.toString(ShortTermPlasticitySynapse.DEFAULT_BASE_LINE_STRENGTH));
//        tfFiringThreshold.setText(Double.toString(ShortTermPlasticitySynapse.DEFAULT_FIRING_THRESHOLD));
//        tfBumpRate.setText(Double.toString(ShortTermPlasticitySynapse.DEFAULT_BUMP_RATE));
//        tfDecayRate.setText(Double.toString(ShortTermPlasticitySynapse.DEFAULT_DECAY_RATE));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        for (int i = 0; i < ruleList.size(); i++) {
            STDPSynapse synapseRef = (STDPSynapse) ruleList.get(i);

            if (!tfTauMinus.getText().equals(NULL_STRING)) {
                synapseRef.setTau_minus(Double.parseDouble(tfTauMinus.getText()));
            }
            if (!tfTauPLus.getText().equals(NULL_STRING)) {
                synapseRef.setTau_plus(Double.parseDouble(tfTauPLus.getText()));
            }
            if (!tfWMinus.getText().equals(NULL_STRING)) {
                synapseRef.setW_minus(Double.parseDouble(tfWMinus.getText()));
            }
            if (!tfWPlus.getText().equals(NULL_STRING)) {
                synapseRef.setW_plus(Double.parseDouble(tfWPlus.getText()));
            }
            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                synapseRef.setLearningRate(Double.parseDouble(tfLearningRate.getText()));
            }
        }
    }
}
