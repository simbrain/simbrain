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
import org.simbrain.network.neuron_update_rules.IACRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>IACNeuronPanel</b>.
 */
public class IACRulePanel extends AbstractNeuronRulePanel {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Decay field. */
    private JTextField tfDecay = new JTextField();

    /** Rest field. */
    private JTextField tfRest = new JTextField();

    /** Random panel. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron update rule being edited. */
    private static final IACRule prototypeRule = new IACRule();

    /**
     * This method is the default constructor.
     *
     */
    public IACRulePanel() {
        super();
        this.add(tabbedPane);
        mainPanel.addItem("Decay", tfDecay);
        mainPanel.addItem("Rest", tfRest);
        mainPanel.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainPanel, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */

    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        IACRule neuronRef = (IACRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Decay
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getDecay"))
            tfDecay.setText(SimbrainConstants.NULL_STRING);
        else
            tfDecay.setText(Double.toString(neuronRef.getDecay()));

        // Handle Rest
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getRest"))
            tfRest.setText(SimbrainConstants.NULL_STRING);
        else
            tfRest.setText(Double.toString(neuronRef.getRest()));

        // Handle Add Noise
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());

        randTab.fillFieldValues(getRandomizers(ruleList));

    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        tfDecay.setText(Double.toString(prototypeRule.getDecay()));
        tfRest.setText(Double.toString(prototypeRule.getRest()));
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof IACRule)) {
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
            IACRule neuronRef = prototypeRule.deepCopy();
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

        // Decay
        double decay = Utils.doubleParsable(tfDecay);
        if (!Double.isNaN(decay)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IACRule) neurons.get(i).getUpdateRule()).setDecay(decay);
            }
        }

        // Rest
        double rest = Utils.doubleParsable(tfRest);
        if (!Double.isNaN(rest)) {
            for (int i = 0; i < numNeurons; i++) {
                ((IACRule) neurons.get(i).getUpdateRule()).setRest(rest);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((IACRule) neurons.get(i).getUpdateRule())
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
    protected IACRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
