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

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.BPTTNetwork;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

import javax.swing.*;

/**
 * Creates a GUI dialog to set the parameters for and then build a BPTT Network.
 *
 * @author Jeff Yoshimi
 */
public class BPTTCreationDialog extends StandardDialog {

    /**
     * Underlying Network Panel
     */
    private final NetworkPanel panel;

    /**
     * Underlying labeled item panel for dialog
     */
    private LabelledItemPanel prefsPanel = new LabelledItemPanel();

    /**
     * Text field for number of input nodes
     */
    private JTextField tfNumInputsOutputs = new JTextField();

    /**
     * Text field for number of hidden layer nodes
     */
    private JTextField tfNumHidden = new JTextField();

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
        prefsPanel.addItem("Number of input / outupt nodes:", tfNumInputsOutputs);
        prefsPanel.addItem("Number of hidden nodes:", tfNumHidden);

        // Fill fields with default values
        tfNumInputsOutputs.setText("" + 5);
        tfNumHidden.setText("" + 5);

        setContentPane(prefsPanel);
    }

    @Override
    public void closeDialogOk() {
        try {

            BPTTNetwork bptt = new BPTTNetwork(panel.getNetwork(), Integer.parseInt(tfNumInputsOutputs.getText()), Integer.parseInt(tfNumHidden.getText()),
                    Integer.parseInt(tfNumInputsOutputs.getText()), panel.getPlacementManager().getLocation());

            bptt.getParentNetwork().addGroup(bptt);
            dispose();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Inappropriate Field Values:" + "\nNetwork construction failed.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
