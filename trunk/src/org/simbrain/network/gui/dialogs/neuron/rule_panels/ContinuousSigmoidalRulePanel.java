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

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;
import org.simbrain.network.neuron_update_rules.ContinuousSigmoidalRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SquashingFunction;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * 
 * @author zach
 *
 */
public class ContinuousSigmoidalRulePanel extends AbstractNeuronPanel {

    /** Implementation combo box. */
    private JComboBox<SquashingFunction> cbImplementation =
            new JComboBox<SquashingFunction>(new SquashingFunction[] {
                SquashingFunction.ARCTAN, SquashingFunction.LOGISTIC,
                SquashingFunction.TANH, });

    /** Time constant field. */
    private JTextField tfTimeConstant = new JTextField();
    
    /** Bias field. */
    private JTextField tfBias = new JTextField();

    /** Slope field. */
    private JTextField tfSlope = new JTextField();

    /** Ceiling */
    private JTextField tfUpbound = new JTextField();

    /** Floor */
    private JTextField tfLowbound = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron rule being edited. */
    private static final ContinuousSigmoidalRule prototypeRule =
            new ContinuousSigmoidalRule();

    /**
     *
     */
    public ContinuousSigmoidalRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Implementation", cbImplementation);
        mainTab.addItem("Time Constant", tfTimeConstant);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Upper Bound", tfUpbound);
        mainTab.addItem("Lower Bound", tfLowbound);
        mainTab.addItem("Add Noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        ContinuousSigmoidalRule neuronRef =
                (ContinuousSigmoidalRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Implementation/Type
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getSquashFunctionType"))
        {
            if ((cbImplementation.getItemCount() == SquashingFunction
                    .values().length - 1))
            {
                cbImplementation.addItem(SquashingFunction.NULL_STRING);
            }
            cbImplementation
            .setSelectedIndex(SquashingFunction.values().length - 1);
        } else {
            cbImplementation.setSelectedItem(neuronRef
                    .getSquashFunctionType());
        }

        // Handle Time Constant
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getTimeConstant"))
        {
            tfTimeConstant.setText(NULL_STRING);
        } else {
            tfTimeConstant.setText(Double.toString(neuronRef.
                    getTimeConstant()));
        }

        // Handle Bias
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getBias"))
        {
            tfBias.setText(NULL_STRING);
        } else {
            tfBias.setText(Double.toString(neuronRef.getBias()));
        }

        // Handle Slope
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getSlope"))
        {
            tfSlope.setText(NULL_STRING);
        } else {
            tfSlope.setText(Double.toString(neuronRef.getSlope()));
        }

        // Handle Lower Value
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getFloor"))
        {
            tfLowbound.setText(NULL_STRING);
        } else {
            tfLowbound.setText(Double.toString(neuronRef.getFloor()));
        }

        // Handle Upper Value
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getCeiling"))
        {
            tfUpbound.setText(NULL_STRING);
        } else {
            tfUpbound.setText(Double.toString(neuronRef.getCeiling()));
        }

        // Handle Noise
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getAddNoise"))
        {
            isAddNoise.setNull();
        } else {
            isAddNoise.setSelected(neuronRef.getAddNoise());
        }

        randTab.fillFieldValues(getRandomizers(ruleList));

    }

    /**
     * Gets all the randomizers associated with some list of neuron update
     * rules.
     * @param ruleList the list of rules from which randomizers are extracted
     * @return List of randomizers.
     */
    private ArrayList<Randomizer> getRandomizers(
            List<NeuronUpdateRule> ruleList) {
        ArrayList<Randomizer> ret = new ArrayList<Randomizer>();

        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((ContinuousSigmoidalRule) ruleList.get(i)).
                    getNoiseGenerator());
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillDefaultValues() {
        cbImplementation.setSelectedItem(prototypeRule
                .getSquashFunctionType());
        tfTimeConstant.setText(Double.toString(prototypeRule
                .getTimeConstant()));
        tfBias.setText(Double.toString(prototypeRule.getBias()));
        tfSlope.setText(Double.toString(prototypeRule.getSlope()));
        tfUpbound.setText(Double.toString(prototypeRule.getCeiling()));
        tfLowbound.setText(Double.toString(prototypeRule.getFloor()));
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof ContinuousSigmoidalRule)) {
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
            ContinuousSigmoidalRule neuronRef = prototypeRule.deepCopy();
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

        // Implementation: Logistic/Tanh/Arctan
        if (!cbImplementation.getSelectedItem().equals(
                SquashingFunction.NULL_STRING))
        {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                .setSquashFunctionType((SquashingFunction)
                        cbImplementation.getSelectedItem());
            }
        }

        // Time Constant
        double timeConstant = Utils.doubleParsable(tfTimeConstant);
        if (!Double.isNaN(timeConstant)) {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule()).
                    setTimeConstant(timeConstant);
            }
        }

        // Bias
        double bias = Utils.doubleParsable(tfBias);
        if (!Double.isNaN(bias)) {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                .setBias(bias);
            }
        }

        // Slope
        double slope = Utils.doubleParsable(tfSlope);
        if (!Double.isNaN(slope)) {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                .setSlope(slope);
            }
        }

        // Lower Value
        double lv = Utils.doubleParsable(tfLowbound);
        if (!Double.isNaN(lv)) {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                .setLowerBound(lv);
            }
        }

        // Upper Value
        double uv = Utils.doubleParsable(tfUpbound);
        if (!Double.isNaN(uv)) {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                .setUpperBound(uv);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise =
                    isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                .setAddNoise(addNoise);
            }
            if (addNoise) {
                for (int i = 0; i < numNeurons; i++) {
                    randTab.commitRandom(((ContinuousSigmoidalRule)
                            neurons.get(i).getUpdateRule())
                                .getNoiseGenerator());
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

}
