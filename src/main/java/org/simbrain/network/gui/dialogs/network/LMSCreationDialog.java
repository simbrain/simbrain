/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.LayeredNetworkCreationPanel.LayerCreationPanel;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.util.StandardDialog;

import javax.swing.*;

/**
 * <b>LMSDialog</b> is a dialog box for creating an LMS network.
 */
public class LMSCreationDialog extends StandardDialog {

    /**
     * Network panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Input layer creation panel.
     */
    private LayerCreationPanel inputLayer;

    /**
     * Target layer creation panel.
     */
    private LayerCreationPanel outputLayer;

    /**
     * This method is the default constructor.
     *
     * @param np Network panel
     */
    public LMSCreationDialog(final NetworkPanel np) {
        networkPanel = np;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("New LMS Network");

        // TODO: These defaults should come from the model via something like
        // fill field values
        Box panel = Box.createVerticalBox();
        inputLayer = new LayerCreationPanel("Input layer", 5);
        inputLayer.setComboBox("Linear");
        outputLayer = new LayerCreationPanel("Output layer", 5);
        outputLayer.setComboBox("Linear");
        panel.add(outputLayer);
        panel.add(inputLayer);
        setContentPane(panel);
        pack();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {

        // Create the layered network
        LMSNetwork lms = new LMSNetwork(networkPanel.getNetwork(), inputLayer.getNumNeurons(), outputLayer.getNumNeurons(), networkPanel.getWhereToAdd());
        lms.getInputLayer().setNeuronType(inputLayer.getNeuronType());
        lms.getOutputLayer().setNeuronType(outputLayer.getNeuronType());
        networkPanel.getNetwork().addGroup(lms);

        networkPanel.repaint();
        super.closeDialogOk();
    }
}
