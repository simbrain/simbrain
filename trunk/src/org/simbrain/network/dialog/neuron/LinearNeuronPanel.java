/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simbrain.network.dialog.neuron;

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.LinearNeuron;


/**
 * <b>LinearNeuronPanel</b>.
 */
public class LinearNeuronPanel extends AbstractNeuronPanel {

    /** Slope field. */
    private JTextField tfSlope = new JTextField();

    /** Bias field. */
    private JTextField tfBias = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomPanel randTab = new RandomPanel(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * Creates an instance of this panel.
     *
     */
    public LinearNeuronPanel() {
        this.add(tabbedPane);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        LinearNeuron neuronRef = (LinearNeuron) neuronList.get(0);

        isAddNoise.setSelected(neuronRef.getAddNoise());
        tfSlope.setText(Double.toString(neuronRef.getSlope()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, LinearNeuron.class, "getSlope")) {
            tfSlope.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, LinearNeuron.class, "getBias")) {
            tfBias.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, LinearNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, LinearNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuronList.size(); i++) {
            ret.add(((LinearNeuron) neuronList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for linear neuron.
     */
    public void fillDefaultValues() {
        LinearNeuron neuronRef = new LinearNeuron();
        tfSlope.setText(Double.toString(neuronRef.getSlope()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            LinearNeuron neuronRef = (LinearNeuron) neuronList.get(i);

            if (!tfSlope.getText().equals(NULL_STRING)) {
                neuronRef.setSlope(Double.parseDouble(tfSlope.getText()));
            }

            if (!tfBias.getText().equals(NULL_STRING)) {
                neuronRef.setBias(Double.parseDouble(tfBias.getText()));
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
