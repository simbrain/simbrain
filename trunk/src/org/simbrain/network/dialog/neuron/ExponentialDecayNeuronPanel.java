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
import org.simnet.interfaces.Network;
import org.simnet.neurons.ExponentialDecayNeuron;

/**
 * <b>ClampedNeuronPanel</b>.
 */
public class ExponentialDecayNeuronPanel extends AbstractNeuronPanel {

    /** Time step field. */
    private JTextField tfTimeStep = new JTextField();

    /** Time constant field. */
    private JTextField tfTimeConstant = new JTextField();


    /**
     * This method is the default constructor.
     * @param net Reference to network
     */
    public ExponentialDecayNeuronPanel(final Network net) {
        parentNet = net;

        addItem("Time step", tfTimeStep);
        addItem("Time constant", tfTimeConstant);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        ExponentialDecayNeuron neuronRef = (ExponentialDecayNeuron) neuronList.get(0);

        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));

//      Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, ExponentialDecayNeuron.class, "getTimeConstant")) {
            tfTimeConstant.setText(NULL_STRING);
        }

    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        ExponentialDecayNeuron neuronRef = new ExponentialDecayNeuron();

        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < neuronList.size(); i++) {
            ExponentialDecayNeuron neuronRef = (ExponentialDecayNeuron) neuronList.get(i);

            if (!tfTimeConstant.getText().equals(NULL_STRING)) {
                neuronRef.setTimeConstant(Double.parseDouble(tfTimeConstant.getText()));
            }
        }
    }
}
