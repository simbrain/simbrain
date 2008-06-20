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
import org.simbrain.network.neurons.StochasticNeuron;


/**
 * <b>StochasticNeuronPanel</b>.
 */
public class StochasticNeuronPanel extends AbstractNeuronPanel {

    /** Firing probability field. */
    private JTextField tfFiringProbability = new JTextField();

    /**
     * Creates an instance of this panel.
     *
     */
    public StochasticNeuronPanel() {
        this.addItem("Firing probability", tfFiringProbability);
        this.addBottomText("<html>\"Firing probability\" is the probability of <p> the neuron's"
                + " state taking on the upper bound value.</html>");
    }

    /**
     * Populates the fields with current data.
     */
    public void fillFieldValues() {
        StochasticNeuron neuronRef = (StochasticNeuron) neuronList.get(0);

        tfFiringProbability.setText(Double.toString(neuronRef.getFiringProbability()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, StochasticNeuron.class, "getFiringProbability")) {
            tfFiringProbability.setText(NULL_STRING);
        }
    }

    /**
     * Populates the fields with default data.
     */
    public void fillDefaultValues() {
        StochasticNeuron neuronRef = new StochasticNeuron();
        tfFiringProbability.setText(Double.toString(neuronRef.getFiringProbability()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            StochasticNeuron neuronRef = (StochasticNeuron) neuronList.get(i);

            if (!tfFiringProbability.getText().equals(NULL_STRING)) {
                neuronRef.setFiringProbability(Double.parseDouble(tfFiringProbability.getText()));
            }
        }
    }
}
