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
import org.simbrain.network.synapse_update_rules.STDPRule;


/**
 * <b>ShortTermPlasticitySynapsePanel</b> allows users to edit STDP synapses.
 */
public class STDPRulePanel extends AbstractSynapsePanel {

    /** Tau minus. */
    private JTextField tfTauMinus = new JTextField();

    /** Tau plus. */
    private JTextField tfTauPlus= new JTextField();

    /** W minus. */
    private JTextField tfWMinus = new JTextField();

    /** WÊplus. */
    private JTextField tfWPlus= new JTextField();

    /** Learning rate. */
    private JTextField tfLearningRate = new JTextField();

    /** Synapse reference. */
    private STDPRule synapseRef;

    /**
     * Creates a short term plasticity synapse panel.
     */
    public STDPRulePanel() {
        this.addItem("Tau minus", tfTauMinus);
        this.addItem("Tau plus", tfTauPlus);
        this.addItem("W+", tfWPlus);
        this.addItem("W-", tfWMinus);
        this.addItem("Learning rate", tfLearningRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (STDPRule) ruleList.get(0);

        tfTauMinus.setText(Double.toString(synapseRef.getTau_minus()));
        tfTauPlus.setText(Double.toString(synapseRef.getTau_plus()));
        tfWMinus.setText(Double.toString(synapseRef.getW_minus()));
        tfWPlus.setText(Double.toString(synapseRef.getW_plus()));
        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));

        // Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getTau_minus")) {
            tfTauMinus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getTau_plus")) {
            tfTauPlus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getW_minus")) {
            tfWMinus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getW_plus")) {
            tfWPlus.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        STDPRule template = new STDPRule();
        tfTauPlus.setText("" +  template.getTau_plus());
        tfTauMinus.setText("" +  template.getTau_minus());
        tfWPlus.setText("" +  template.getW_plus());
        tfWMinus.setText("" +  template.getW_minus());
        tfLearningRate.setText("" +  template.getLearningRate());
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        for (int i = 0; i < ruleList.size(); i++) {
            STDPRule synapseRef = (STDPRule) ruleList.get(i);

            if (!tfTauMinus.getText().equals(NULL_STRING)) {
                synapseRef.setTau_minus(Double.parseDouble(tfTauMinus.getText()));
            }
            if (!tfTauPlus.getText().equals(NULL_STRING)) {
                synapseRef.setTau_plus(Double.parseDouble(tfTauPlus.getText()));
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
