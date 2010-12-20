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
import org.simbrain.network.neurons.BinaryNeuron;
import org.simbrain.network.neurons.HodgkinHuxleyNeuron;
import org.simbrain.util.LabelledItemPanel;


/**
 * <b>BinaryNeuronPanel</b> creates a dialog for setting preferences of binary neurons.
 */
public class HodgkinHuxleyNeuronPanel extends AbstractNeuronPanel {

    private JTextField perNaChannels = new JTextField();
    
    private JTextField perKChannels = new JTextField();
    
    private JTextField getEna = new JTextField();
    
    private JTextField getEk = new JTextField();
    
//    private JTextField ENA = new JTextField();
//
//    private JTextField Ek = new JTextField();
    
    /** Main tab for neuron prefernces. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * Creates HodgkinHuxley preferences panel.
     */
    public HodgkinHuxleyNeuronPanel(RootNetwork network) {
        super(network);
        this.add(mainTab);
        mainTab.addItem("Sodium Channels", perNaChannels);        
        mainTab.addItem("Potassium Channels", perKChannels);
        mainTab.addItem("Equilibrium Potential", getEna);
        mainTab.addItem("Equilibrium Potential", getEk);
        }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        HodgkinHuxleyNeuron neuronRef = (HodgkinHuxleyNeuron) ruleList.get(0);

        perNaChannels.setText(Double.toString(neuronRef.getPerNaChannels()));
        perKChannels.setText(Double.toString(neuronRef.getPerKChannels()));
//        ENA.setText(Double.toString(neuronRef.getENA()));

        
        
//        //Handle consistency of multiple selections
//        if (!NetworkUtils.isConsistent(ruleList, HodgkinHuxleyNeuron.class, "getTemp")) {
//            tfTemp.setText(NULL_STRING);
//        }
        
    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        HodgkinHuxleyNeuron neuronRef = new HodgkinHuxleyNeuron();
//        tfTemp.setText(Double.toString(neuronRef.getTemp()));

    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            HodgkinHuxleyNeuron neuronRef = (HodgkinHuxleyNeuron) ruleList.get(i);

//          if (!tfTemp.getText().equals(NULL_STRING)) {
//             neuronRef.setTemp(Double.parseDouble(tfTemp.getText()));
//         }

        }
    }
}
