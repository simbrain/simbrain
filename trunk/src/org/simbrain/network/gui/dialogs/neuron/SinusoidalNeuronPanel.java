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
package org.simbrain.network.gui.dialogs.neuron;

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.neurons.SinusoidalNeuron;
import org.simbrain.network.util.RandomSource;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;


/**
 * <b>SinusoidalNeuronPanel</b>.
 */
public class SinusoidalNeuronPanel extends AbstractNeuronPanel {

    /** Phase field. */
    private JTextField tfPhase = new JTextField();

    /** Frequency field. */
    private JTextField tfFrequency = new JTextField();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Random panel. */
    private RandomPanel randPanel = new RandomPanel(true);

    /** Tabbed panel. */
    private JTabbedPane tabbedPanel = new JTabbedPane();

    /**
     * Creates an instance of this panel.
     *
     */
    public SinusoidalNeuronPanel(RootNetwork network) {
        super(network);
        this.add(tabbedPanel);
        mainPanel.addItem("Phase", tfPhase);
        mainPanel.addItem("Frequency", tfFrequency);
        mainPanel.addItem("Add noise", isAddNoise);
        tabbedPanel.add(mainPanel, "Main");
        tabbedPanel.add(randPanel, "Noise");
    }

    /**
     * Populates the field with current data.
     */
    public void fillFieldValues() {
        SinusoidalNeuron neuronRef = (SinusoidalNeuron) ruleList.get(0);

        tfFrequency.setText(Double.toString(neuronRef.getFrequency()));
        tfPhase.setText(Double.toString(neuronRef.getPhase()));
        isAddNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, SinusoidalNeuron.class, "getFrequency")) {
            tfFrequency.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, SinusoidalNeuron.class, "getPhase")) {
            tfPhase.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, SinusoidalNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }
        randPanel.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList<RandomSource> getRandomizers() {
        ArrayList<RandomSource> ret = new ArrayList<RandomSource>();

        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((SinusoidalNeuron) ruleList.get(i)).getNoiseGenerator());
        }

        return ret;
    }


    /**
     * Populates the fields with default data.
     */
    public void fillDefaultValues() {
        SinusoidalNeuron neuronRef = new SinusoidalNeuron();
        tfFrequency.setText(Double.toString(neuronRef.getFrequency()));
        tfPhase.setText(Double.toString(neuronRef.getPhase()));
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randPanel.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            SinusoidalNeuron neuronRef = (SinusoidalNeuron) ruleList.get(i);

            if (!tfPhase.getText().equals(NULL_STRING)) {
                neuronRef.setPhase(Double.parseDouble(tfPhase.getText()));
            }
            if (!tfFrequency.getText().equals(NULL_STRING)) {
                neuronRef.setFrequency(Double.parseDouble(tfFrequency.getText()));
            }
            if (!isAddNoise.isNull()) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }

            randPanel.commitRandom(neuronRef.getNoiseGenerator());

        }
    }
}
