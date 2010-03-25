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

import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neurons.ThreeValuedNeuron;
import org.simbrain.util.LabelledItemPanel;


/**
 * <b>ThreeValuedNeuronPanel</b> creates a dialog for setting preferences of three valued neurons.
 */
public class ThreeValuedNeuronPanel extends AbstractNeuronPanel {

    /** Threshold for this neuron. */
    private JTextField tfLowerThreshold = new JTextField();

    /** Bias for this neuron. */
    private JTextField tfBias = new JTextField();

    /** Upper threshold field. */
    private JTextField tfUpperThreshold = new JTextField();

    /** Lower value field. */
    private JTextField tfLowerValue = new JTextField();

    /** Middle value field. */
    private JTextField tfMiddleValue = new JTextField();

    /** Upper value field. */
    private JTextField tfUpperValue = new JTextField();

    /** Main tab for neuron prefernces. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * Creates binary neuron preferences panel.
     */
    public ThreeValuedNeuronPanel() {
        this.add(mainTab);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Lower threshold", tfLowerThreshold);
        mainTab.addItem("Upper threshold", tfUpperThreshold);
        mainTab.addItem("Lower value", tfLowerValue);
        mainTab.addItem("Middle value", tfMiddleValue);
        mainTab.addItem("Upper value", tfUpperValue);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        ThreeValuedNeuron neuronRef = (ThreeValuedNeuron) neuronList.get(0);

        tfLowerThreshold.setText(Double.toString(neuronRef.getLowerThreshold()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfUpperThreshold.setText(Double.toString(neuronRef.getUpperThreshold()));
        tfUpperValue.setText(Double.toString(neuronRef.getUpperValue()));
        tfMiddleValue.setText(Double.toString(neuronRef.getMiddleValue()));
        tfLowerValue.setText(Double.toString(neuronRef.getLowerValue()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, ThreeValuedNeuron.class, "getLowerThreshold")) {
            tfLowerThreshold.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, ThreeValuedNeuron.class, "getBias")) {
            tfBias.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, ThreeValuedNeuron.class, "getUpperThreshold")) {
            tfUpperThreshold.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, ThreeValuedNeuron.class, "getLowerValue")) {
            tfLowerValue.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, ThreeValuedNeuron.class, "getMiddleValue")) {
            tfMiddleValue.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, ThreeValuedNeuron.class, "getUpperValue")) {
            tfUpperValue.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        ThreeValuedNeuron neuronRef = new ThreeValuedNeuron();
        tfLowerThreshold.setText(Double.toString(neuronRef.getLowerThreshold()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfUpperThreshold.setText(Double.toString(neuronRef.getUpperThreshold()));
        tfLowerValue.setText(Double.toString(neuronRef.getLowerValue()));
        tfMiddleValue.setText(Double.toString(neuronRef.getMiddleValue()));
        tfUpperValue.setText(Double.toString(neuronRef.getUpperValue()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            ThreeValuedNeuron neuronRef = (ThreeValuedNeuron) neuronList.get(i);

            if (!tfLowerThreshold.getText().equals(NULL_STRING)) {
                neuronRef.setLowerThreshold(Double.parseDouble(tfLowerThreshold.getText()));
            }
            if (!tfBias.getText().equals(NULL_STRING)) {
                neuronRef.setBias(Double.parseDouble(tfBias.getText()));
            }
            if (!tfUpperThreshold.getText().equals(NULL_STRING)) {
                neuronRef.setUpperThreshold(Double.parseDouble(tfUpperThreshold.getText()));
            }
            if (!tfLowerValue.getText().equals(NULL_STRING)) {
                neuronRef.setLowerValue(Double.parseDouble(tfLowerValue.getText()));
            }
            if (!tfMiddleValue.getText().equals(NULL_STRING)) {
                neuronRef.setMiddleValue(Double.parseDouble(tfMiddleValue.getText()));
            }
            if (!tfUpperValue.getText().equals(NULL_STRING)) {
                neuronRef.setUpperValue(Double.parseDouble(tfUpperValue.getText()));
            }
        }
    }
}
