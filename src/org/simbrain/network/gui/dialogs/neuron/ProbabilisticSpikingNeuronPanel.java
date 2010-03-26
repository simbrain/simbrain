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

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.neurons.IntegrateAndFireNeuron;
import org.simbrain.network.neurons.ProbabilisticSpikingNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;


/**
 * <b>ProbabilisticSpikingNeuronPanel</b>.
 */
public class ProbabilisticSpikingNeuronPanel extends AbstractNeuronPanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Time step field. */
    private JTextField tfFireProbability = new JTextField();

    /** Random tab. */
    private RandomPanel randTab = new RandomPanel(true);

    /**
     * Creates a new instance of the probabilistic spiking neuron panel.
     * @param net Network
     */
    public ProbabilisticSpikingNeuronPanel(final Network net) {
        parentNet = net;

        this.add(tabbedPane);
        mainTab.addItem("Fire Probability", tfFireProbability);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
    	ProbabilisticSpikingNeuron neuronRef = (ProbabilisticSpikingNeuron) neuronList.get(0);

    	tfFireProbability.setText(Double.toString(neuronRef.getFireProbability()));
    	
    	//Handle consistency of multiple selections
    	if (!NetworkUtils.isConsistent(neuronList, ProbabilisticSpikingNeuron.class, "getFireProbability")) {
            tfFireProbability.setText(NULL_STRING);
        }
    	
        }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        ProbabilisticSpikingNeuron neuronRef = new ProbabilisticSpikingNeuron();
        tfFireProbability.setText(Double.toString(neuronRef.getFireProbability()));
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        
    	for (int i = 0; i < neuronList.size(); i++) {
            ProbabilisticSpikingNeuron neuronRef = (ProbabilisticSpikingNeuron) neuronList.get(i);
            
            if (!tfFireProbability.getText().equals(NULL_STRING)) {
                neuronRef.setFireProbability(Double.parseDouble(tfFireProbability.getText()));
            }
        }
    }
}

