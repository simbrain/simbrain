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
package org.simbrain.network.dialog.neuron;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.neurons.TemporalDifferenceNeuron;

/**
 * <b>TemporalDifferenceNeuronPanel</b>.
 */
public class TemporalDifferenceNeuronPanel extends AbstractNeuronPanel {

    /** Alpha field. */
    private JTextField tfAlpha = new JTextField();

    /** Beta field. */
    private JTextField tfBeta = new JTextField();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     */
    public TemporalDifferenceNeuronPanel() {
        add(mainTab);
        mainTab.addItem("Alpha", tfAlpha);
        mainTab.addItem("Beta", tfBeta);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        TemporalDifferenceNeuron neuronRef = (TemporalDifferenceNeuron) neuronList.get(0);

        tfAlpha.setText(Double.toString(neuronRef.getAlpha()));
        tfBeta.setText(Double.toString(neuronRef.getBeta()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, TemporalDifferenceNeuron.class, "getAlpha")) {
            tfAlpha.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, TemporalDifferenceNeuron.class, "getBeta")) {
            tfBeta.setText(NULL_STRING);
        }
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        TemporalDifferenceNeuron neuronRef = new TemporalDifferenceNeuron();
        tfAlpha.setText(Double.toString(neuronRef.getAlpha()));
        tfBeta.setText(Double.toString(neuronRef.getBeta()));

    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            TemporalDifferenceNeuron neuronRef = (TemporalDifferenceNeuron) neuronList.get(i);

            if (!tfAlpha.getText().equals(NULL_STRING)) {
                neuronRef.setAlpha(Double.parseDouble(tfAlpha.getText()));
            }
            if (!tfBeta.getText().equals(NULL_STRING)) {
                neuronRef.setBeta(Double.parseDouble(tfBeta.getText()));
            }
        }
    }
}
