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
package org.simbrain.network.gui.dialogs.connect;

import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * Dialog for using connection objects to create connections between loose neurons.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public class ConnectionDialog extends StandardDialog {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * The main panel.
     */
    private final ConnectionPanel connectionPanel;

    /**
     * Construct the dialog.
     *
     * @param networkPanel parent panel
     * @param connection   the underlying connection object
     */
    public ConnectionDialog(final NetworkPanel networkPanel, final ConnectionStrategy connection) {
        this.networkPanel = networkPanel;
        this.connectionPanel = new ConnectionPanel(this, connection,
                networkPanel.getSelectedModels(Neuron.class).size(), ConnectionUtilities.
                testRecurrence(networkPanel.getSelectedModels(Neuron.class),
                        networkPanel.getSelectionManager().sourceModelsOf(Neuron.class)), true);
        setContentPane(connectionPanel);
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Network/connections.html");
        addButton(new JButton(helpAction));
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        connectionPanel.commitChanges(networkPanel);
    }

}