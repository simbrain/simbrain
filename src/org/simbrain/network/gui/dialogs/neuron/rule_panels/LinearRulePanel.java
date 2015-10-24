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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronNoiseGenPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>LinearNeuronPanel</b>.
 */
public class LinearRulePanel extends AbstractNeuronRulePanel {

    /** Slope field. */
    private JTextField tfSlope = new JTextField();

    /** Bias field. */
    private JTextField tfBias = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron update rule being edited. */
    private static final LinearRule prototypeRule = new LinearRule();

    /**
     * Creates an instance of this panel.
     *
     */
    public LinearRulePanel() {
        this.add(tabbedPane);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        LinearRule neuronRef = (LinearRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Slope
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class, "getSlope"))
            tfSlope.setText(SimbrainConstants.NULL_STRING);
        else
            tfSlope.setText(Double.toString(neuronRef.getSlope()));

        // Handle Bias
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class, "getBias"))
            tfBias.setText(SimbrainConstants.NULL_STRING);
        else
            tfBias.setText(Double.toString(neuronRef.getBias()));

        // Handle Noise
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class,
                "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());

        randTab.fillFieldValues(getRandomizers(ruleList));

    }

    /**
     * Fill field values to default values for linear neuron.
     */
    public void fillDefaultValues() {
        tfSlope.setText(Double.toString(prototypeRule.getSlope()));
        tfBias.setText(Double.toString(prototypeRule.getBias()));
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof LinearRule)) {
            neuron.setUpdateRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(neuron));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(List<Neuron> neurons) {

        if (isReplace()) {
            LinearRule neuronRef = prototypeRule.deepCopy();
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
    protected void writeValuesToRules(List<Neuron> neurons) {
        int numNeurons = neurons.size();

        // Slope
        double slope = Utils.doubleParsable(tfSlope);
        if (!Double.isNaN(slope)) {
            for (int i = 0; i < numNeurons; i++) {
                ((LinearRule) neurons.get(i).getUpdateRule()).setSlope(slope);
            }
        }

        // Bias
        double bias = Utils.doubleParsable(tfBias);
        if (!Double.isNaN(bias)) {
            for (int i = 0; i < numNeurons; i++) {
                ((LinearRule) neurons.get(i).getUpdateRule()).setBias(bias);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((LinearRule) neurons.get(i).getUpdateRule())
                        .setAddNoise(addNoise);
            }
            if (addNoise) {
                randTab.commitRandom(neurons);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
