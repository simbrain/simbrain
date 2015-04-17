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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;

/**
 * <b>BinaryNeuronPanel</b> creates a dialog for setting preferences of binary
 * neurons.
 */
public class BinaryRulePanel extends AbstractNeuronRulePanel {

    /** Threshold for this neuron. */
    private JTextField tfThreshold = new JTextField();

    /** Ceiling */
    private JTextField tfUpbound = new JTextField();

    /** Floor */
    private JTextField tfLowbound = new JTextField();

    /** Bias for this neuron. */
    private JTextField tfBias = new JTextField();

    /** Main tab for neuron prefernces. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron rule being edited. */
    private static final BinaryRule prototypeRule = new BinaryRule();

    /**
     * Creates binary neuron preferences panel.
     */
    public BinaryRulePanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        mainTab.addItem("Threshold", tfThreshold);
        mainTab.addItem("On Value", tfUpbound);
        mainTab.addItem("Off Value", tfLowbound);
        mainTab.addItem("Bias", tfBias);
        mainTab.setAlignmentX(CENTER_ALIGNMENT);
        this.add(mainTab);
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        BinaryRule neuronRef = (BinaryRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Threshold
        if (!NetworkUtils.isConsistent(ruleList, BinaryRule.class,
                "getThreshold"))
            tfThreshold.setText(SimbrainConstants.NULL_STRING);
        else
            tfThreshold.setText(Double.toString(neuronRef.getThreshold()));

        // Handle Lower Value
        if (!NetworkUtils.isConsistent(ruleList, BinaryRule.class, "getLowerBound"))
            tfLowbound.setText(SimbrainConstants.NULL_STRING);
        else
            tfLowbound.setText(Double.toString(neuronRef.getLowerBound()));

        // Handle Upper Value
        if (!NetworkUtils
                .isConsistent(ruleList, BinaryRule.class, "getUpperBound"))
            tfUpbound.setText(SimbrainConstants.NULL_STRING);
        else
            tfUpbound.setText(Double.toString(neuronRef.getUpperBound()));

        // Handle Bias
        if (!NetworkUtils.isConsistent(ruleList, BinaryRule.class, "getBias"))
            tfBias.setText(SimbrainConstants.NULL_STRING);
        else
            tfBias.setText(Double.toString(neuronRef.getBias()));

    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        tfThreshold.setText(Double.toString(prototypeRule.getThreshold()));
        tfUpbound.setText(Double.toString(prototypeRule.getUpperBound()));
        tfLowbound.setText(Double.toString(prototypeRule.getLowerBound()));
        tfBias.setText(Double.toString(prototypeRule.getBias()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof BinaryRule)) {
            neuron.setUpdateRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(neuron));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final List<Neuron> neurons) {

        if (isReplace()) {
            BinaryRule neuronRef = prototypeRule.deepCopy();
            for (Neuron n : neurons) {
                n.setUpdateRule(neuronRef.deepCopy());
            }
        }

        writeValuesToRules(neurons);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeValuesToRules(final List<Neuron> neurons) {

        int numNeurons = neurons.size();

        // Threshold
        double threshold = Utils.doubleParsable(tfThreshold);
        if (!Double.isNaN(threshold)) {
            for (int i = 0; i < numNeurons; i++) {
                ((BinaryRule) neurons.get(i).getUpdateRule())
                        .setThreshold(threshold);
            }
        }

        // Lower Value
        double lv = Utils.doubleParsable(tfLowbound);
        if (!Double.isNaN(lv)) {
            for (int i = 0; i < numNeurons; i++) {
                ((BinaryRule) neurons.get(i).getUpdateRule()).setFloor(lv);
            }
        }

        // Upper Value
        double uv = Utils.doubleParsable(tfUpbound);
        if (!Double.isNaN(uv)) {
            for (int i = 0; i < numNeurons; i++) {
                ((BinaryRule) neurons.get(i).getUpdateRule()).setCeiling(uv);
            }
        }

        // Bias
        double bias = Utils.doubleParsable(tfBias);
        if (!Double.isNaN(bias)) {
            for (int i = 0; i < numNeurons; i++) {
                ((BinaryRule) neurons.get(i).getUpdateRule()).setBias(bias);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BinaryRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
