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
package org.simbrain.network.gui.dialogs.synapse.plasticity_panels;

import java.util.Collections;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.AbstractSynapsePanel;
import org.simbrain.network.synapse_update_rules.STDPRule;
import org.simbrain.util.Utils;

/**
 * <b>ShortTermPlasticitySynapsePanel</b> allows users to edit STDP synapses.
 */
public class STDPRulePanel extends AbstractSynapsePanel {

    /** Tau minus. */
    private final JTextField tfTauMinus = new JTextField();

    /** Tau plus. */
    private final JTextField tfTauPlus = new JTextField();

    /** W minus. */
    private final JTextField tfWMinus = new JTextField();

    /** W plus. */
    private final JTextField tfWPlus = new JTextField();

    /** Learning rate. */
    private final JTextField tfLearningRate = new JTextField();

    /** Synapse reference. */
    private static final STDPRule prototypeRule = new STDPRule();

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
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {

        STDPRule synapseRef = (STDPRule) ruleList.get(0);

        // (Below) Handle consistency of multiply selections

        // Handle Tau Minus
        if (!NetworkUtils
                .isConsistent(ruleList, STDPRule.class, "getTau_minus")) {
            tfTauMinus.setText(NULL_STRING);
        } else {
            tfTauMinus.setText(Double.toString(synapseRef.getTau_minus()));
        }

        // Handle Tau Plus
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getTau_plus")) {
            tfTauPlus.setText(NULL_STRING);
        } else {
            tfTauPlus.setText(Double.toString(synapseRef.getTau_plus()));
        }

        // Handle W Minus
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getW_minus")) {
            tfWMinus.setText(NULL_STRING);
        } else {
            tfWMinus.setText(Double.toString(synapseRef.getW_minus()));
        }

        // Handle W Plus
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class, "getW_plus")) {
            tfWPlus.setText(NULL_STRING);
        } else {
            tfWPlus.setText(Double.toString(synapseRef.getW_plus()));
        }

        // Handle Learning Rate
        if (!NetworkUtils.isConsistent(ruleList, STDPRule.class,
                "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        } else {
            tfLearningRate
                    .setText(Double.toString(synapseRef.getLearningRate()));
        }

    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        STDPRule template = new STDPRule();
        tfTauPlus.setText("" + template.getTau_plus());
        tfTauMinus.setText("" + template.getTau_minus());
        tfWPlus.setText("" + template.getW_plus());
        tfWMinus.setText("" + template.getW_minus());
        tfLearningRate.setText("" + template.getLearningRate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Synapse synapse) {

        if (!(synapse.getLearningRule() instanceof STDPRule)) {
            synapse.setLearningRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(synapse));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final List<Synapse> synapses) {
        if (isReplace()) {
            for (Synapse s : synapses) {
                s.setLearningRule(prototypeRule.deepCopy());
            }
        }

        writeValuesToRules(synapses);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeValuesToRules(List<Synapse> synapses) {

        // Tau Minus
        double tauMinus = Utils.doubleParsable(tfTauMinus);
        if (!Double.isNaN(tauMinus)) {
            for (Synapse s : synapses) {
                ((STDPRule) s.getLearningRule()).setTau_minus(tauMinus);
            }
        }

        // Tau Plus
        double tauPlus = Utils.doubleParsable(tfTauPlus);
        if (!Double.isNaN(tauPlus)) {
            for (Synapse s : synapses) {
                ((STDPRule) s.getLearningRule()).setTau_plus(tauPlus);
            }
        }

        // W Minus
        double wMinus = Utils.doubleParsable(tfWMinus);
        if (!Double.isNaN(wMinus)) {
            for (Synapse s : synapses) {
                ((STDPRule) s.getLearningRule()).setW_minus(wMinus);
            }
        }

        // W Plus
        double wPlus = Utils.doubleParsable(tfWPlus);
        if (!Double.isNaN(wPlus)) {
            for (Synapse s : synapses) {
                ((STDPRule) s.getLearningRule()).setW_plus(wPlus);
            }
        }

        // Learning Rate
        double learningRate = Utils.doubleParsable(tfLearningRate);
        if (!Double.isNaN(learningRate)) {
            for (Synapse s : synapses) {
                ((STDPRule) s.getLearningRule()).setLearningRate(learningRate);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynapseUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

}
