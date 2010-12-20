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
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.neurons.RunningAverageNeuron;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>RunningAverageNeuronPanel</b>.
 */
public class RunningAverageNeuronPanel extends AbstractNeuronPanel {

    /** Rate constant field. */
    private JTextField tfRateConstant = new JTextField();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     */
    public RunningAverageNeuronPanel(RootNetwork network) {
        super(network);
        add(mainTab);
        mainTab.addItem("Rate Constant", tfRateConstant);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        RunningAverageNeuron neuronRef = (RunningAverageNeuron) ruleList.get(0);

        tfRateConstant.setText(Double.toString(neuronRef.getRateConstant()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, RunningAverageNeuron.class, "getRateConstant")) {
            tfRateConstant.setText(NULL_STRING);
        }
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        RunningAverageNeuron neuronRef = new RunningAverageNeuron();
        tfRateConstant.setText(Double.toString(neuronRef.getRateConstant()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            RunningAverageNeuron neuronRef = (RunningAverageNeuron) ruleList.get(i);

            if (!tfRateConstant.getText().equals(NULL_STRING)) {
                neuronRef.setRateConstant(Double.parseDouble(tfRateConstant.getText()));
            }
        }
    }
}
