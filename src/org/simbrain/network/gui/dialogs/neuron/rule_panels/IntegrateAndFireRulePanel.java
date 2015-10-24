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
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>IntegrateAndFireNeuronPanel</b>.
 */
public class IntegrateAndFireRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Time constant field. */
    private JTextField tfTimeConstant = new JTextField();

    /** Threshold field. */
    private JTextField tfThreshold = new JTextField();

    /** Reset field. */
    private JTextField tfReset = new JTextField();

    /** Resistance field. */
    private JTextField tfResistance = new JTextField();

    /** Resting potential field. */
    private JTextField tfRestingPotential = new JTextField();

    /** Background current field. */
    private JTextField tfBackgroundCurrent = new JTextField();

    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron update rule being edited. */
    private static final IntegrateAndFireRule prototypeRule =
        new IntegrateAndFireRule();

    /**
     * Creates a new instance of the integrate and fire neuron panel.
     */
    public IntegrateAndFireRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Threshold (mV)", tfThreshold);
        mainTab.addItem("Resting potential (mV)", tfRestingPotential);
        mainTab.addItem("Reset potential (mV)", tfReset);
        mainTab.addItem("Resistance (M\u03A9)", tfResistance);
        mainTab.addItem("Background Current (nA)", tfBackgroundCurrent);
        mainTab.addItem("Time constant (ms)", tfTimeConstant);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        IntegrateAndFireRule neuronRef = (IntegrateAndFireRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Resting Potential
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getRestingPotential"))
            tfRestingPotential.setText(SimbrainConstants.NULL_STRING);
        else
            tfRestingPotential.setText(Double.toString(neuronRef
                .getRestingPotential()));

        // Handle Resistance
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getResistance"))
            tfResistance.setText(SimbrainConstants.NULL_STRING);
        else
            tfResistance.setText(Double.toString(neuronRef.getResistance()));

        // Handle Add Noise
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());

        // Handle Reset Potential
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getResetPotential"))
            tfReset.setText(SimbrainConstants.NULL_STRING);
        else
            tfReset.setText(Double.toString(neuronRef.getResetPotential()));

        // Handle Time Constant
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getTimeConstant"))
            tfTimeConstant.setText(SimbrainConstants.NULL_STRING);
        else
            tfTimeConstant
                .setText(Double.toString(neuronRef.getTimeConstant()));

        // Handle Threshold
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getThreshold"))
            tfThreshold.setText(SimbrainConstants.NULL_STRING);
        else
            tfThreshold.setText(Double.toString(neuronRef.getThreshold()));

        // Handle Background Current
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
            "getBackgroundCurrent"))
            tfBackgroundCurrent.setText(SimbrainConstants.NULL_STRING);
        else
            tfBackgroundCurrent.setText(Double.toString(neuronRef
                .getBackgroundCurrent()));

        randTab.fillFieldValues(getRandomizers(ruleList));

    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        tfRestingPotential.setText(Double.toString(prototypeRule
            .getRestingPotential()));
        tfResistance.setText(Double.toString(prototypeRule.getResistance()));
        tfReset.setText(Double.toString(prototypeRule.getResetPotential()));
        tfThreshold.setText(Double.toString(prototypeRule.getThreshold()));
        tfBackgroundCurrent.setText(Double.toString(prototypeRule
            .getBackgroundCurrent()));
        tfTimeConstant
            .setText(Double.toString(prototypeRule.getTimeConstant()));
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof IntegrateAndFireRule)) {
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
            IntegrateAndFireRule neuronRef = prototypeRule.deepCopy();
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

        // Time Constant
        double timeConstant = Utils.doubleParsable(tfTimeConstant);
        if (!Double.isNaN(timeConstant)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
                    .setTimeConstant(timeConstant);
            }
        }

        // Threshold
        double threshold = Utils.doubleParsable(tfThreshold);
        if (!Double.isNaN(threshold)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
                    .setThreshold(threshold);
            }
        }

        // Background Current
        double backgroundCurrent = Utils.doubleParsable(tfBackgroundCurrent);
        if (!Double.isNaN(backgroundCurrent)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
                    .setBackgroundCurrent(backgroundCurrent);
            }
        }

        // Reset
        double reset = Utils.doubleParsable(tfReset);
        if (!Double.isNaN(reset)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
                    .setResetPotential(reset);
            }
        }

        // Resistance
        double resistance = Utils.doubleParsable(tfResistance);
        if (!Double.isNaN(resistance)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
                    .setResistance(resistance);
            }
        }

        // Resting Potential
        double restingPotential = Utils.doubleParsable(tfRestingPotential);
        if (!Double.isNaN(restingPotential)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
                    .setRestingPotential(restingPotential);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise =
                isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
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
    protected IntegrateAndFireRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
