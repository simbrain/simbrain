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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.AbstractSynapseRulePanel;
import org.simbrain.network.synapse_update_rules.ShortTermPlasticityRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>ShortTermPlasticitySynapsePanel</b>.
 */
public class ShortTermPlasticityRulePanel extends AbstractSynapseRulePanel {

    /** Baseline strength field. */
    private final JTextField tfBaseLineStrength = new JTextField();

    /** Firing threshold field. */
    private final JTextField tfFiringThreshold = new JTextField();

    /** Bump rate field. */
    private final JTextField tfBumpRate = new JTextField();

    /** Decay rate field. */
    private final JTextField tfDecayRate = new JTextField();

    /** Plasticity type combo box. */
    private final YesNoNull cbPlasticityType = new YesNoNull(
        "Depression", "Facilitation");

    /** Synapse reference. */
    private static final ShortTermPlasticityRule prototypeRule =
        new ShortTermPlasticityRule();

    /**
     * Creates a short term plasticity synapse panel.
     */
    public ShortTermPlasticityRulePanel() {
        this.addItem("Plasticity type", cbPlasticityType);
        this.addItem("Base-line-strength", tfBaseLineStrength);
        this.addItem("Firing threshold", tfFiringThreshold);
        this.addItem("Growth-rate", tfBumpRate);
        this.addItem("Decay-rate", tfDecayRate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShortTermPlasticityRulePanel deepCopy() {
        ShortTermPlasticityRulePanel copy = new ShortTermPlasticityRulePanel();
        copy.tfBaseLineStrength.setText(tfBaseLineStrength.getText());
        copy.tfFiringThreshold.setText(tfFiringThreshold.getText());
        copy.tfBumpRate.setText(tfBumpRate.getText());
        copy.tfDecayRate.setText(tfDecayRate.getText());
        copy.cbPlasticityType.setSelectedIndex(cbPlasticityType
            .getSelectedIndex());
        return copy;
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {

        ShortTermPlasticityRule synapseRef = (ShortTermPlasticityRule) ruleList
            .get(0);

        // (Below) Handle consistency of multiply selections

        // Handle Plasticity Type
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
            "getPlasticityType")) {
            cbPlasticityType.setNull();
        } else {
            cbPlasticityType.setSelectedIndex(synapseRef.getPlasticityType());
        }

        // Handle Base Line Strength
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
            "getBaseLineStrength")) {
            tfBaseLineStrength.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfBaseLineStrength.setText(Double.toString(synapseRef
                .getBaseLineStrength()));
        }

        // Handle Firing Threshold
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
            "getFiringThreshold")) {
            tfFiringThreshold.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfFiringThreshold.setText(Double.toString(synapseRef
                .getFiringThreshold()));
        }

        // Handle Bump Rate
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
            "getBumpRate")) {
            tfBumpRate.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfBumpRate.setText(Double.toString(synapseRef.getBumpRate()));
        }

        // Handle Decay Rate
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
            "getDecayRate")) {
            tfDecayRate.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfDecayRate.setText(Double.toString(synapseRef.getDecayRate()));
        }

    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {

        cbPlasticityType
            .setSelectedIndex(ShortTermPlasticityRule.DEFAULT_PLASTICITY_TYPE);
        tfBaseLineStrength.setText(Double
            .toString(ShortTermPlasticityRule.DEFAULT_BASE_LINE_STRENGTH));
        tfFiringThreshold.setText(Double
            .toString(ShortTermPlasticityRule.DEFAULT_FIRING_THRESHOLD));
        tfBumpRate.setText(Double
            .toString(ShortTermPlasticityRule.DEFAULT_BUMP_RATE));
        tfDecayRate.setText(Double
            .toString(ShortTermPlasticityRule.DEFAULT_DECAY_RATE));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Synapse synapse) {

        if (!(synapse.getLearningRule() instanceof ShortTermPlasticityRule)) {
            synapse.setLearningRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(synapse));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Collection<Synapse> synapses) {
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
    protected void writeValuesToRules(Collection<Synapse> synapses) {

        // Plasticity Type
        if (!cbPlasticityType.isNull()) {
            for (Synapse s : synapses) {
                ((ShortTermPlasticityRule) s.getLearningRule())
                    .setPlasticityType(cbPlasticityType.getSelectedIndex());
            }
        }

        // Baseline Strength
        double baseLineStrength = Utils.doubleParsable(tfBaseLineStrength);
        if (!Double.isNaN(baseLineStrength)) {
            for (Synapse s : synapses) {
                ((ShortTermPlasticityRule) s.getLearningRule())
                    .setBaseLineStrength(baseLineStrength);
            }
        }

        // Firing Threshold
        double firingThreshold = Utils.doubleParsable(tfFiringThreshold);
        if (!Double.isNaN(firingThreshold)) {
            for (Synapse s : synapses) {
                ((ShortTermPlasticityRule) s.getLearningRule())
                    .setFiringThreshold(firingThreshold);
            }
        }

        // Bump Rate
        double bumpRate = Utils.doubleParsable(tfBumpRate);
        if (!Double.isNaN(bumpRate)) {
            for (Synapse s : synapses) {
                ((ShortTermPlasticityRule) s.getLearningRule())
                    .setBumpRate(bumpRate);
            }
        }

        // Decay Rate
        double decayRate = Utils.doubleParsable(tfDecayRate);
        if (!Double.isNaN(decayRate)) {
            for (Synapse s : synapses) {
                ((ShortTermPlasticityRule) s.getLearningRule())
                    .setDecayRate(decayRate);
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
