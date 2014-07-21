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
import java.util.Collections;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SquashingFunction;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>SigmoidalRulePanel</b>. A rule panel for editing neurons which use
 * a discrete sigmoid squashing function as their update rule.
 * 
 * TODO: Discuss renaming to "DiscreteSigmoidalRulePanel".
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class SigmoidalRulePanel extends AbstractSigmoidalRulePanel {

    /** A reference to the neuron rule being edited. */
    private static SigmoidalRule prototypeRule;

    /**
     * Creates a fully functional discrete sigmoidal rule panel.
     * 
     * @return
     */
    public static SigmoidalRulePanel createSigmoidalRulePanel() {
        prototypeRule = new SigmoidalRule();
        final SigmoidalRulePanel dsrp = new SigmoidalRulePanel();
        dsrp.cbImplementation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SquashingFunction currentFunc = (SquashingFunction)
                    dsrp.cbImplementation.getSelectedItem();
                if (!currentFunc.equals(dsrp.initialSfunction)) {
                    prototypeRule.setSquashFunctionType(currentFunc);
                    dsrp.fillDefaultValues();
                }
                dsrp.repaint();
            }
        });
        dsrp.fillDefaultValues();
        return dsrp;
    }

    /**
     * Creates the discrete sigmoidal rule panel, but does not initialize the
     * listeners responsible for altering the panel in response to the selected
     * squashing function.
     */
    private SigmoidalRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Implementation", cbImplementation);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Add Noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        SigmoidalRule neuronRef =
            (SigmoidalRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Implementation/Type
        if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
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
        // Handle Bias
        if (!NetworkUtils
            .isConsistent(ruleList, SigmoidalRule.class, "getBias"))
        {
            tfBias.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfBias.setText(Double.toString(neuronRef.getBias()));
        }
        // Handle Slope
        if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
            "getSlope"))
        {
            tfSlope.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfSlope.setText(Double.toString(neuronRef.getSlope()));
        }

        // Handle Noise
        if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
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
     * Fill field values to default values for sigmoidal neuron.
     */
    public void fillDefaultValues() {
        cbImplementation.setSelectedItem(prototypeRule.getSquashFunctionType());
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

        if (!(neuron.getUpdateRule() instanceof SigmoidalRule)) {
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
            SigmoidalRule neuronRef = prototypeRule.deepCopy();
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
                ((SigmoidalRule) neurons.get(i).getUpdateRule())
                    .setSquashFunctionType((SquashingFunction)
                    cbImplementation.getSelectedItem());
            }
        }

        // Bias
        double bias = Utils.doubleParsable(tfBias);
        if (!Double.isNaN(bias)) {
            for (int i = 0; i < numNeurons; i++) {
                ((SigmoidalRule) neurons.get(i).getUpdateRule())
                    .setBias(bias);
            }
        }

        // Slope
        double slope = Utils.doubleParsable(tfSlope);
        if (!Double.isNaN(slope)) {
            for (int i = 0; i < numNeurons; i++) {
                ((SigmoidalRule) neurons.get(i).getUpdateRule())
                    .setSlope(slope);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise =
                isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((SigmoidalRule) neurons.get(i).getUpdateRule())
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
