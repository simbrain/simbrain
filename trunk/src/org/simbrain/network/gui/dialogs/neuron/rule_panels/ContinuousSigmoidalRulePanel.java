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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronNoiseGenPanel;
import org.simbrain.network.neuron_update_rules.ContinuousSigmoidalRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SquashingFunction;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 *
 * @author zach
 *
 */
public class ContinuousSigmoidalRulePanel extends AbstractNeuronRulePanel {

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

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron rule being edited. */
    private static final ContinuousSigmoidalRule prototypeRule =
            new ContinuousSigmoidalRule();

    /**
     * The initially selected squashing function (or NULL_STRING), used for
     * determining how to fill field values based on the selected
     * implementation.
     */
    private SquashingFunction initialSfunction;

    /** The upper bound for whatever state the panel starts in. */
    private String initialUBound;

    /** The lower bound for whatever state the panel starts in. */
    private String initialLBound;

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
        mainTab.addItem("Add Noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
        cbImplementation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SquashingFunction currentFunc = (SquashingFunction)
                        cbImplementation.getSelectedItem();
                if (!currentFunc.equals(initialSfunction)) {
                    prototypeRule.setSquashFunctionType(currentFunc);
                    fillDefaultValues();
                }
                repaint();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        ContinuousSigmoidalRule neuronRef = (ContinuousSigmoidalRule) ruleList
                .get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Implementation/Type
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getSquashFunctionType"))
        {
            if ((cbImplementation.getItemCount()
                    == SquashingFunction.values().length - 1))
            {
                cbImplementation.addItem(SquashingFunction.NULL_STRING);
            }
            cbImplementation
                    .setSelectedIndex(SquashingFunction.values().length - 1);
        } else {
            cbImplementation.setSelectedItem(neuronRef.getSquashFunctionType());
        }

        // Handle Time Constant
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getTimeConstant"))
        {
            tfTimeConstant.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfTimeConstant
                    .setText(Double.toString(neuronRef.getTimeConstant()));
        }

        // Handle Bias
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getBias"))
        {
            tfBias.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfBias.setText(Double.toString(neuronRef.getBias()));
        }

        // Handle Slope
        if (!NetworkUtils.isConsistent(ruleList, ContinuousSigmoidalRule.class,
                "getSlope"))
        {
            tfSlope.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfSlope.setText(Double.toString(neuronRef.getSlope()));
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
        initialSfunction = (SquashingFunction) cbImplementation.
                getSelectedItem();
    }

    /**
     * Gets all the randomizers associated with some list of neuron update
     * rules.
     *
     * @param ruleList the list of rules from which randomizers are extracted
     * @return List of randomizers.
     */
    private ArrayList<Randomizer> getRandomizers(
            List<NeuronUpdateRule> ruleList) {
        ArrayList<Randomizer> ret = new ArrayList<Randomizer>();

        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((ContinuousSigmoidalRule) ruleList.get(i))
                    .getNoiseGenerator());
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillDefaultValues() {
        cbImplementation.setSelectedItem(prototypeRule.getSquashFunctionType());
        tfTimeConstant
                .setText(Double.toString(prototypeRule.getTimeConstant()));
        tfBias.setText(Double.toString(prototypeRule.getBias()));
        tfSlope.setText(Double.toString(prototypeRule.getSlope()));
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
                                cbImplementation
                                .getSelectedItem());
            }
        }

        // Time Constant
        double timeConstant = Utils.doubleParsable(tfTimeConstant);
        if (!Double.isNaN(timeConstant)) {
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
                        .setTimeConstant(timeConstant);
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

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((ContinuousSigmoidalRule) neurons.get(i).getUpdateRule())
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
        return prototypeRule;
    }

}