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

import javax.swing.*;

/**
 * <b>BackpropDialog</b> is a dialog box for creating a Backprop network.
 */
public class BackpropCreationDialog extends FeedForwardCreationDialog {

    /**
     * Construct the dialog.  Called using reflection by AddGroupAction.
     *
     * @param np Network panel
     */
    public BackpropCreationDialog(final NetworkPanel np) {
        super(np);
        setTitle("New Backprop Network");
    }

    @Override
    protected void closeDialogOk() {
        try {
            networkCreationPanel.commit(networkPanel, "Backprop");
            dispose();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Inappropriate Field Values (Numbers only in all all field)", "Error", JOptionPane.ERROR_MESSAGE);
            nfe.printStackTrace();
        }
    }


}
