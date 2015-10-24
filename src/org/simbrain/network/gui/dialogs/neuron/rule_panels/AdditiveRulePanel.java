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
import org.simbrain.network.neuron_update_rules.AdditiveRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.randomizer.gui.RandomizerPanel;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>AdditiveNeuronPanel</b>. TODO: No implementation... why?
 */
public class AdditiveRulePanel extends AbstractNeuronRulePanel {

    /** Lambda field. */
    private JTextField tfLambda = new JTextField();

    /** Resistance field. */
    private JTextField tfResistance = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomizerPanel randTab = new RandomizerPanel();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron update rule being edited. */
    private static final AdditiveRule prototypeRule = new AdditiveRule();

    /**
     * Creates an instance of this panel.
     */
    public AdditiveRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Lambda", tfLambda);
        mainTab.addItem("Resistance", tfResistance);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    @Override
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        AdditiveRule neuronRef = (AdditiveRule) ruleList.get(0);

        tfLambda.setText(Double.toString(neuronRef.getLambda()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        // isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        // Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getLambda")) {
            tfLambda.setText(SimbrainConstants.NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getResistance")) {
            tfResistance.setText(SimbrainConstants.NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers(ruleList));
    }

    /**
     * Fill field values to default values for additive neuron.
     */
    public void fillDefaultValues() {
        tfLambda.setText(Double.toString(prototypeRule.getLambda()));
        tfResistance.setText(Double.toString(prototypeRule.getResistance()));
        // isClipping.setSelected(prototypeRule.getClipping());
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
	 *
	 */
    @Override
    public void commitChanges(final Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof AdditiveRule)) {
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
            AdditiveRule neuronRef = prototypeRule.deepCopy();
            for (Neuron n : neurons) {
                n.setUpdateRule(neuronRef);
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

        // Lambda
        double lambda = Utils.doubleParsable(tfLambda);
        if (!Double.isNaN(lambda)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdditiveRule) neurons.get(i).getUpdateRule())
                        .setLambda(lambda);
            }
        }

        // Resistance
        double resistance = Utils.doubleParsable(tfResistance);
        if (!Double.isNaN(resistance)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdditiveRule) neurons.get(i).getUpdateRule())
                        .setResistance(resistance);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((AdditiveRule) neurons.get(i).getUpdateRule())
                        .setAddNoise(addNoise);

            }
            if (addNoise) {
                for (int i = 0; i < numNeurons; i++) {
                    randTab.commitRandom(((AdditiveRule) neurons.get(i)
                            .getUpdateRule()).getNoiseGenerator());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected AdditiveRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
