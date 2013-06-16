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
package org.simbrain.network.gui.dialogs.network;

import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule.SigmoidType;
import org.simbrain.network.subnetworks.BPTTNetwork;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Creates a GUI dialog to set the parameters for and then build a BPTT Network.
 *
 * @author Jeff Yoshimi
 */
public class BPTTCreationDialog extends StandardDialog {

    /** Underlying Network Panel */
    private final NetworkPanel panel;

    /** Underlying labeled item panel for dialog */
    private LabelledItemPanel srnPanel = new LabelledItemPanel();

    /** Text field for number of input nodes */
    private JTextField tfNumInputsOutputs = new JTextField();

    /** Text field for number of hidden layer nodes */
    private JTextField tfNumHidden = new JTextField();

    /**
     * Maps string values to corresponding NeuronUpdateRules for the combo-boxes
     * governing desired Neuron type for a given layer
     */
    private HashMap<String, NeuronUpdateRule> boxMap = new HashMap<String, NeuronUpdateRule>();

    /**
     * Mapping of Strings to NeuronUpdateRules, currently only Logisitc, Tanh,
     * and Linear neurons are allowed.
     */
    {
        boxMap.put("Linear", new LinearRule());
        SigmoidalRule sig0 = new SigmoidalRule();
        sig0.setType(SigmoidType.LOGISTIC);
        boxMap.put("Logistic", sig0);
        SigmoidalRule sig1 = new SigmoidalRule();
        sig1.setType(SigmoidType.TANH);
        boxMap.put("Tanh", sig1);
    }

    /** String values for combo-boxes (same as key values for boxMap) */
    private String[] options = { "Linear", "Tanh", "Logistic" };

    /** Combo box for selecting update rule for the hidden layer */
    private JComboBox hiddenNeuronTypes = new JComboBox(options);

    /** Combo box for selecting the update rule for the output layer */
    private JComboBox outputNeuronTypes = new JComboBox(options);

    /**
     * Constructs a labeled item panel dialog for the creation of a simple
     * recurrent network.
     *
     * @param panel the network panel the BPTT will be tied to
     */
    public BPTTCreationDialog(final NetworkPanel panel) {
        this.panel = panel;

        setTitle("Build Backprop Through Time Network");

        // Add fields
        tfNumInputsOutputs.setColumns(5);
        srnPanel.addItem("Number of input / outupt nodes:", tfNumInputsOutputs);
        // srnPanel.addItem("Hidden Neuron Type:", hiddenNeuronTypes, 2);
        srnPanel.addItem("Number of hidden nodes:", tfNumHidden);
        // srnPanel.addItem("Output Neuron Type:", outputNeuronTypes, 2);

        // Fill fields with default values
        fillFieldValues();

        setContentPane(srnPanel);
    }

    /**
     * Fills the fields with default values.
     */
    public void fillFieldValues() {
        tfNumInputsOutputs.setText("" + 5);
        tfNumHidden.setText("" + 5);
        hiddenNeuronTypes.setSelectedIndex(2);
    }

    @Override
    public void closeDialogOk() {
        try {

            NeuronUpdateRule hidType = boxMap.get(hiddenNeuronTypes
                    .getSelectedItem());
            NeuronUpdateRule outType = boxMap.get(outputNeuronTypes
                    .getSelectedItem());
            BPTTNetwork bptt = new BPTTNetwork(panel.getNetwork(),
                    Integer.parseInt(tfNumInputsOutputs.getText()),
                    Integer.parseInt(tfNumHidden.getText()),
                    Integer.parseInt(tfNumInputsOutputs.getText()),
                    panel.getLastClickedPosition());

            bptt.getParentNetwork().addGroup(bptt);
            srnPanel.setVisible(false);
            dispose();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Inappropriate Field Values:"
                    + "\nNetwork construction failed.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        panel.repaint();
    }

}
