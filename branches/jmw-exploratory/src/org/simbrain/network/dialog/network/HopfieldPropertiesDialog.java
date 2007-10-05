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
package org.simbrain.network.dialog.network;

import javax.swing.JComboBox;
import javax.swing.JButton;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.Hopfield;
import org.simbrain.network.actions.ShowHelpAction;

/**
 * <b>DiscreteHopfieldPropertiesDialog</b> is a dialog box for setting the properties of a discrete hopfield network.
 */
public class HopfieldPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Sequential network update order. */
    public static final int SEQUENTIAL = 0;

    /** Random network update order. */
    public static final int RANDOM = 1;

    /** Network type combo box. */
    private JComboBox cbUpdateOrder = new JComboBox(new String[] {"Sequential", "Random" });

    /** The model subnetwork. */
    private Hopfield hop;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /**
     * Default constructor.
     *
     * @param hop Discrete hopfield network being modified.
     */
    public HopfieldPropertiesDialog(final Hopfield hop) {
        this.hop = hop;
        setTitle("Set Discrete Hopfield Properties");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog
        helpAction.setTheURL("Network/network/hopfieldnetwork.html");
        helpButton.setAction(helpAction);

        this.addButton(helpButton);
        mainPanel.addItem("Update order", cbUpdateOrder);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        hop.setUpdateOrder(getType());
        super.closeDialogOk();
    }

    /**
     * @return the update order.
     */
    public int getType() {
        if (cbUpdateOrder.getSelectedIndex() == 0) {
            return SEQUENTIAL;
        } else {
            return RANDOM;
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        cbUpdateOrder.setSelectedIndex(hop.getUpdateOrder());
    }

}
