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
import org.simbrain.network.synapse_update_rules.HebbianThresholdRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>HebbianThresholdSynapsePanel</b>.
 */
public class HebbianThresholdRulePanel extends AbstractSynapseRulePanel {

    /** Learning rate field. */
    private final JTextField tfLearningRate = new JTextField();

    /** Output threshold momentum field. */
    private final JTextField tfOutputThresholdMomentum = new JTextField();

    /** Output threshold. */
    private final JTextField tfOutputThreshold = new JTextField();

    /** Output threshold combo box. */
    private final YesNoNull isOutputThresholdSliding =
        new YesNoNull();

    /** Synapse refernece. */
    private static final HebbianThresholdRule prototypeRule =
        new HebbianThresholdRule();

    /**
     * This method is the default constructor.
     */
    public HebbianThresholdRulePanel() {
        this.addItem("Learning rate", tfLearningRate);
        this.addItem("Threshold", tfOutputThreshold);
        this.addItem("Threshold Momentum", tfOutputThresholdMomentum);
        this.addItem("Sliding Threshold", isOutputThresholdSliding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HebbianThresholdRulePanel deepCopy() {
        HebbianThresholdRulePanel copy = new HebbianThresholdRulePanel();
        copy.tfLearningRate.setText(tfLearningRate.getText());
        copy.tfOutputThreshold.setText(tfOutputThreshold.getText());
        copy.tfOutputThresholdMomentum.setText(tfOutputThresholdMomentum
            .getText());
        copy.isOutputThresholdSliding.setSelectedIndex(isOutputThresholdSliding
            .getSelectedIndex());
        return copy;
    }

    /**
     * Populate fields with current data.
     *
     * @param ruleList
     *            the list of rules to edit/use to display variables from
     */
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {

        HebbianThresholdRule synapseRef = (HebbianThresholdRule) ruleList
            .get(0);

        // (Below) Handle consistency of multiply selections

        // Handle Learning Rate
        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdRule.class,
            "getLearningRate")) {
            tfLearningRate.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfLearningRate
                .setText(Double.toString(synapseRef.getLearningRate()));
        }

        // Handle Threshold Momentum
        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdRule.class,
            "getOutputThresholdMomentum")) {
            tfOutputThresholdMomentum.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfOutputThresholdMomentum.setText(Double.toString(synapseRef
                .getOutputThresholdMomentum()));
        }

        // Handle Output Threshold
        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdRule.class,
            "getOutputThreshold")) {
            tfOutputThreshold.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfOutputThreshold.setText(Double.toString(synapseRef
                .getOutputThreshold()));
        }

        // Handle Output Threshold Slider
        if (!NetworkUtils.isConsistent(ruleList, HebbianThresholdRule.class,
            "getUseSlidingOutputThreshold")) {
            isOutputThresholdSliding.setNull();
        } else {
            isOutputThresholdSliding.setSelected(synapseRef
                .getUseSlidingOutputThreshold());
        }

    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        tfLearningRate.setText(Double
            .toString(HebbianThresholdRule.DEFAULT_LEARNING_RATE));
        tfOutputThresholdMomentum
            .setText(Double
                .toString(HebbianThresholdRule.DEFAULT_OUTPUT_THRESHOLD_MOMENTUM));
        tfOutputThreshold.setText(Double
            .toString(HebbianThresholdRule.DEFAULT_OUTPUT_THRESHOLD));
        isOutputThresholdSliding
            .setSelected(HebbianThresholdRule.DEFAULT_USE_SLIDING_OUTPUT_THRESHOLD);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Synapse synapse) {

        if (!(synapse.getLearningRule() instanceof HebbianThresholdRule)) {
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
    protected void writeValuesToRules(final Collection<Synapse> synapses) {

        // LearningRate
        double learningRate = Utils.doubleParsable(tfLearningRate);
        if (!Double.isNaN(learningRate)) {
            for (Synapse s : synapses) {
                ((HebbianThresholdRule) s.getLearningRule())
                    .setLearningRate(learningRate);
            }
        }

        // Output Threshold Momentum
        double tm = Utils.doubleParsable(tfOutputThresholdMomentum);
        if (!Double.isNaN(tm)) {
            for (Synapse s : synapses) {
                ((HebbianThresholdRule) s.getLearningRule())
                    .setOutputThresholdMomentum(tm);
            }
        }

        // Output threshold
        double outputThreshold = Utils.doubleParsable(tfOutputThreshold);
        if (!Double.isNaN(outputThreshold)) {
            for (Synapse s : synapses) {
                ((HebbianThresholdRule) s.getLearningRule())
                    .setOutputThreshold(tm);
            }
        }

        // Sliding Output Threshold
        if (!isOutputThresholdSliding.isNull()) {
            boolean slidingThreshold = isOutputThresholdSliding.isSelected();
            for (Synapse s : synapses) {
                ((HebbianThresholdRule) s.getLearningRule())
                    .setUseSlidingOutputThreshold(slidingThreshold);
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
