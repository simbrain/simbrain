/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

import org.simbrain.network.NetworkUtils;

import org.simnet.neurons.LogisticNeuron;

import javax.swing.JTextField;


/**
 * <b>LogisticNeuronPanel</b>
 */
public class LogisticNeuronPanel extends AbstractNeuronPanel {
    private JTextField tfGrowthRate = new JTextField();

    public LogisticNeuronPanel() {
        addItem("Growth rate", tfGrowthRate);

        this.addBottomText("<html>Note: for chaos, growth rates between <p> 3.6 and 4 are reccomended </html>");
    }

    public void fillFieldValues() {
        LogisticNeuron neuron_ref = (LogisticNeuron) neuron_list.get(0);

        tfGrowthRate.setText(Double.toString(neuron_ref.getGrowthRate()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuron_list, LogisticNeuron.class, "getGrowthRate")) {
            tfGrowthRate.setText(NULL_STRING);
        }
    }

    public void fillDefaultValues() {
        LogisticNeuron neuronRef = new LogisticNeuron();
        tfGrowthRate.setText(Double.toString(neuronRef.getGrowthRate()));
    }

    public void commitChanges() {
        for (int i = 0; i < neuron_list.size(); i++) {
            LogisticNeuron neuronRef = (LogisticNeuron) neuron_list.get(i);

            if (tfGrowthRate.getText().equals(NULL_STRING) == false) {
                neuronRef.setGrowthRate(Double.parseDouble(tfGrowthRate.getText()));
            }
        }
    }
}
