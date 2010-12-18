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
import org.simbrain.network.neurons.IACNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;


/**
 * <b>IACNeuronPanel</b>.
 */
public class IACNeuronPanel extends AbstractNeuronPanel {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Decay field. */
    private JTextField tfDecay = new JTextField();

    /** Rest field. */
    private JTextField tfRest = new JTextField();

    /** Random panel. */
    private RandomPanel randTab = new RandomPanel(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * This method is the default constructor.
     *
     */
    public IACNeuronPanel() {
        this.add(tabbedPane);
        mainPanel.addItem("Decay", tfDecay);
        mainPanel.addItem("Rest", tfRest);
        mainPanel.addItem("Use clipping", isClipping);
        mainPanel.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainPanel, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        IACNeuron neuronRef = (IACNeuron) ruleList.get(0);

        tfDecay.setText(Double.toString(neuronRef.getDecay()));
        tfRest.setText(Double.toString(neuronRef.getRest()));
        isAddNoise.setSelected(neuronRef.getAddNoise());
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, IACNeuron.class, "getDecay")) {
            tfDecay.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, IACNeuron.class, "getRest")) {
            tfRest.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, IACNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(ruleList, IACNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((IACNeuron) ruleList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        IACNeuron neuronRef = new IACNeuron();
        tfDecay.setText(Double.toString(neuronRef.getDecay()));
        tfRest.setText(Double.toString(neuronRef.getRest()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            IACNeuron neuronRef = (IACNeuron) ruleList.get(i);

            if (!tfDecay.getText().equals(NULL_STRING)) {
                neuronRef.setDecay(Double.parseDouble(tfDecay.getText()));
            }

            if (!tfRest.getText().equals(NULL_STRING)) {
                neuronRef.setRest(Double.parseDouble(tfRest.getText()));
            }

            if (!isClipping.isNull()) {
                neuronRef.setClipping(isClipping.isSelected());
            }

            if (!isAddNoise.isNull()) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }

            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }
}
