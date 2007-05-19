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
import org.simnet.neurons.TraceNeuron;

/**
 * <b>ClampedNeuronPanel</b>.
 */
public class TraceNeuronPanel extends AbstractNeuronPanel {

    /** C1 field. */
    private JTextField tfC1 = new JTextField();

    /** C2 field. */
    private JTextField tfC2 = new JTextField();

    /**
     * This method is the default constructor.
     */
    public TraceNeuronPanel() {
        this.addItem("C1", tfC1);
        this.addItem("C2", tfC2);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        TraceNeuron neuronRef = (TraceNeuron) neuronList.get(0);

        tfC1.setText(Double.toString(neuronRef.getC1()));
        tfC2.setText(Double.toString(neuronRef.getC2()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, TraceNeuron.class, "getC1")) {
            tfC1.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, TraceNeuron.class, "getC2")) {
            tfC2.setText(NULL_STRING);
        }
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        TraceNeuron neuronRef = new TraceNeuron();

        tfC1.setText(Double.toString(neuronRef.getC1()));
        tfC2.setText(Double.toString(neuronRef.getC2()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            TraceNeuron neuronRef = (TraceNeuron) neuronList.get(i);

            if (!tfC1.getText().equals(NULL_STRING)) {
                neuronRef.setC1(Double.parseDouble(tfC1.getText()));
            }

            if (!tfC2.getText().equals(NULL_STRING)) {
                neuronRef.setC2(Double.parseDouble(tfC2.getText()));
            }
        }
    }
}
