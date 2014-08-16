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
import org.simbrain.network.synapse_update_rules.SubtractiveNormalizationRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;

/**
 * <b>SubtractiveNormalizationSynapsePanel</b>.
 */
public class SubtractiveNormalizationRulePanel extends
    AbstractSynapseRulePanel {

    /** Learning rate field. */
    private final JTextField tfLearningRate = new JTextField();

    /** Synapse reference. */
    private static final SubtractiveNormalizationRule prototypeRule =
        new SubtractiveNormalizationRule();

    /**
     * This method is the default constructor.
     */
    public SubtractiveNormalizationRulePanel() {
        addItem("Learning rate", tfLearningRate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubtractiveNormalizationRulePanel deepCopy() {
        SubtractiveNormalizationRulePanel copy =
            new SubtractiveNormalizationRulePanel();
        copy.tfLearningRate.setText(tfLearningRate.getText());
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {

        SubtractiveNormalizationRule synapseRef =
            (SubtractiveNormalizationRule) ruleList
                .get(0);

        // (Below) Handle consistency of multiply selections

        // Handle Learning Rate
        if (!NetworkUtils.isConsistent(ruleList,
            SubtractiveNormalizationRule.class, "getLearningRate")) {
            tfLearningRate.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfLearningRate
                .setText(Double.toString(synapseRef.getLearningRate()));
        }

    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        tfLearningRate.setText(Double
            .toString(SubtractiveNormalizationRule.DEFAULT_LEARNING_RATE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Synapse synapse) {

        if (!(synapse.getLearningRule() instanceof SubtractiveNormalizationRule)) {
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

        // Learning Rate
        double learningRate = Utils.doubleParsable(tfLearningRate);
        if (!Double.isNaN(learningRate)) {
            for (Synapse s : synapses) {
                ((SubtractiveNormalizationRule) s.getLearningRule())
                    .setLearningRate(learningRate);
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
