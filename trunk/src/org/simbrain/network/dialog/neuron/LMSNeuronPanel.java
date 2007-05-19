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
import org.simnet.neurons.LMSNeuron;

/**
 * <b>ClampedNeuronPanel</b>.
 */
public class LMSNeuronPanel extends AbstractNeuronPanel {

    /** Learning rate field. */
    private JTextField tfLearningRate = new JTextField();

    /** Main tab for neuron prefernces. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     */
    public LMSNeuronPanel() {
        this.add(mainTab);
        mainTab.addItem("Learning rate", tfLearningRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        LMSNeuron neuronRef = (LMSNeuron) neuronList.get(0);

        tfLearningRate.setText(Double.toString(neuronRef.getLearningRate()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, LMSNeuron.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        LMSNeuron neuronRef = new LMSNeuron();
        tfLearningRate.setText(Double.toString(neuronRef.getLearningRate()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            LMSNeuron neuronRef = (LMSNeuron) neuronList.get(i);

            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                neuronRef.setLearningRate(Double.parseDouble(tfLearningRate.getText()));
            }
        }
    }
}
