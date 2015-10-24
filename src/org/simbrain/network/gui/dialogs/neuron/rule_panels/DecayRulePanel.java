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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>DecayNeuronPanel</b>.
 */
public class DecayRulePanel extends AbstractNeuronRulePanel implements
        ActionListener, PropertyChangeListener {

    /** Relative absolute combo box. */
    private TristateDropDown cbRelAbs = new TristateDropDown("Relative",
            "Absolute");

    /** Decay amount field. */
    private JTextField tfDecayAmount = new JTextField();

    /** Decay fraction field. */
    private JTextField tfDecayFraction = new JTextField();

    /** Base line field. */
    private JTextField tfBaseLine = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    {
        randTab.addPropertyChangeListener(this);
    }

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron update rule being edited. */
    private static final DecayRule prototypeRule = new DecayRule();

    /**
     * This method is the default constructor.
     */
    public DecayRulePanel() {
        super();
        cbRelAbs.addActionListener(this);
        cbRelAbs.setActionCommand("relAbs");

        this.add(tabbedPane);
        mainTab.addItem("", cbRelAbs);
        mainTab.addItem("Base line", tfBaseLine);
        mainTab.addItem("Decay amount", tfDecayAmount);
        mainTab.addItem("Decay fraction", tfDecayFraction);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
        checkBounds();
    }

    /**
     * Responds to actions performed.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("relAbs")) {
            checkBounds();
            this.firePropertyChange("dummy", null, null);
        }
    }

    /**
     * Checks the relative absolute bounds.
     */
    private void checkBounds() {
        if (cbRelAbs.getSelectedIndex() == 0) {
            tfDecayAmount.setEnabled(false);
            tfDecayFraction.setEnabled(true);
        } else {
            tfDecayFraction.setEnabled(false);
            tfDecayAmount.setEnabled(true);
        }
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        DecayRule neuronRef = (DecayRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle Relative Absolute
        if (!NetworkUtils.isConsistent(ruleList, DecayRule.class, "getRelAbs"))
            cbRelAbs.setNull();
        else
            cbRelAbs.setSelectedIndex(neuronRef.getRelAbs());

        // Handle Baseline
        if (!NetworkUtils
                .isConsistent(ruleList, DecayRule.class, "getBaseLine"))
            tfBaseLine.setText(SimbrainConstants.NULL_STRING);
        else
            tfBaseLine.setText(Double.toString(neuronRef.getBaseLine()));

        // Handle Decay Fraction
        if (!NetworkUtils.isConsistent(ruleList, DecayRule.class,
                "getDecayFraction"))
            tfDecayFraction.setText(SimbrainConstants.NULL_STRING);
        else
            tfDecayFraction.setText(Double.toString(neuronRef
                    .getDecayFraction()));

        // Handle Decay Amount
        if (!NetworkUtils.isConsistent(ruleList, DecayRule.class,
                "getDecayAmount"))
            tfDecayAmount.setText(SimbrainConstants.NULL_STRING);
        else
            tfDecayAmount.setText(Double.toString(neuronRef.getDecayAmount()));

        // Handle Noise
        if (!NetworkUtils
                .isConsistent(ruleList, DecayRule.class, "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());

        randTab.fillFieldValues(getRandomizers(ruleList));

    }

    /**
     * Fill field values to default values for additive neuron.
     */
    public void fillDefaultValues() {
        cbRelAbs.setSelectedIndex(prototypeRule.getRelAbs());
        tfBaseLine.setText(Double.toString(prototypeRule.getBaseLine()));
        tfDecayAmount
                .setText(Double.toString(prototypeRule.getDecayFraction()));
        tfDecayFraction.setText(Double.toString(prototypeRule
                .getDecayFraction()));
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof DecayRule)) {
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
            DecayRule neuronRef = prototypeRule.deepCopy();
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

        // Relative/Absolute
        if (!cbRelAbs.isNull()) {
            for (int i = 0; i < numNeurons; i++) {
                ((DecayRule) neurons.get(i).getUpdateRule()).setRelAbs(cbRelAbs
                        .getSelectedIndex());
            }
        }

        // Decay Amount
        double decayAmount = Utils.doubleParsable(tfDecayAmount);
        if (!Double.isNaN(decayAmount)) {
            for (int i = 0; i < numNeurons; i++) {
                ((DecayRule) neurons.get(i).getUpdateRule())
                        .setDecayAmount(decayAmount);
            }
        }

        // Decay Fraction
        double decayFraction = Utils.doubleParsable(tfDecayFraction);
        if (!Double.isNaN(decayFraction)) {
            for (int i = 0; i < numNeurons; i++) {
                ((DecayRule) neurons.get(i).getUpdateRule())
                        .setDecayFraction(decayFraction);
            }
        }

        // Decay Baseline
        double baseLine = Utils.doubleParsable(tfBaseLine);
        if (!Double.isNaN(baseLine)) {
            for (int i = 0; i < numNeurons; i++) {
                ((DecayRule) neurons.get(i).getUpdateRule())
                        .setBaseLine(baseLine);
            }
        }

        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((DecayRule) neurons.get(i).getUpdateRule())
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
    protected DecayRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        this.firePropertyChange("", null, null);
    }

}
