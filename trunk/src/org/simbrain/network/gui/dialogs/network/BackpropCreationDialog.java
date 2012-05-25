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

import java.awt.geom.Point2D;

import javax.swing.JTextField;

import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>BackpropDialog</b> is a dialog box for creating a Backprop network
 */
public class BackpropCreationDialog extends StandardDialog {

    /** Network Topology. */
    private JTextField networkTopology = new JTextField();

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     * @param np Network panel
     */
    public BackpropCreationDialog(final NetworkPanel np) {
        networkPanel = np;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New Backprop Network");

        fillFieldValues();

        LabelledItemPanel panel = new LabelledItemPanel();
        panel.addItem("Topology", networkTopology);
        setContentPane(panel);
    }

    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        networkTopology.setText("5,3,5");
    }
    
    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        int[] topology;
        String[] parsedString = networkTopology.getText().split(",");
        topology = new int[parsedString.length];
        for (int i = 0; i < parsedString.length; i++) {
            topology[i] = Integer.parseInt(parsedString[i]);
        }

        // Get last clicked position in the panel
        Point2D lastClicked = networkPanel.getLastClickedPosition();

        // Create the layered network
        networkPanel.getNetwork().addGroup(new BackpropNetwork(networkPanel.getNetwork(), 
                topology, lastClicked));
        
      networkPanel.repaint();
      super.closeDialogOk();
    }
}
