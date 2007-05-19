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

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.neurons.BinaryNeuron;


/**
 * <b>BinaryNeuronPanel</b> creates a dialog for setting preferences of binary neurons.
 */
public class BinaryNeuronPanel extends AbstractNeuronPanel {

    /** Threshold for this neuron. */
    private JTextField tfThreshold = new JTextField();

    /** Bias for this neuron. */
    private JTextField tfBias = new JTextField();

    /** Main tab for neuron prefernces. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * Creates binary neuron preferences panel.
     */
    public BinaryNeuronPanel() {
        this.add(mainTab);
        mainTab.addItem("Threshold", tfThreshold);
        mainTab.addItem("Bias", tfBias);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        BinaryNeuron neuronRef = (BinaryNeuron) neuronList.get(0);

        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfBias.setText(Double.toString(neuronRef.getBias()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, BinaryNeuron.class, "getThreshold")) {
            tfThreshold.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, BinaryNeuron.class, "getBias")) {
            tfBias.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        BinaryNeuron neuronRef = new BinaryNeuron();
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            BinaryNeuron neuronRef = (BinaryNeuron) neuronList.get(i);

            if (!tfThreshold.getText().equals(NULL_STRING)) {
                neuronRef.setThreshold(Double.parseDouble(tfThreshold.getText()));
            }
            if (!tfBias.getText().equals(NULL_STRING)) {
                neuronRef.setBias(Double.parseDouble(tfBias.getText()));
            }
        }
    }
}
