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
package org.simbrain.network.gui.actions.connection;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.Radial;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.ConditionallyEnabledAction;
import org.simbrain.network.gui.dialogs.connect.AbstractConnectionPanel;
import org.simbrain.network.gui.dialogs.connect.ConnectionDialog;
import org.simbrain.network.gui.dialogs.connect.connector_panels.DensityBasedConnectionPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.OneToOnePanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.RadialPanel;

/**
 * Apply specified connection either from selected neurons to themselves
 * ("self connect") or from source to target.
 */
@SuppressWarnings("serial")
public final class ApplyConnectionAction extends ConditionallyEnabledAction {

    /** The connection to apply. */
    private ConnectNeurons connection;

    /** Connection panel for dialog. */
    private AbstractConnectionPanel optionsPanel = null;

    /**
     * Construct the action.
     * 
     * @param networkPanel
     *            networkPanel, must not be null
     * @param connection
     *            the connection to apply
     * @param name
     *            the name of this action
     * @param isSelfConnect
     *            whether to connect selected neurons to themselves or not.
     */
    public ApplyConnectionAction(final NetworkPanel networkPanel,
        ConnectNeurons connection, String name) {

        super(networkPanel, name, EnablingCondition.SOURCE_AND_TARGET_NEURONS);

        putValue(SHORT_DESCRIPTION, "Use " + name
            + " method to connect source to target neurons");

        this.connection = connection;

    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        String title = "Connect ";

        if (connection instanceof AllToAll) {
            optionsPanel =
                DensityBasedConnectionPanel.createAllToAllAdjustmentPanel(
                    (AllToAll) connection, networkPanel);
            title += "All to All";
        } else if (connection instanceof OneToOne) {
            optionsPanel = new OneToOnePanel((OneToOne) connection);
            title += "One to One";
        } else if (connection instanceof Radial) {
            optionsPanel = new RadialPanel((Radial) connection);
            title += "Radial";
        } else if (connection instanceof Sparse) {
            optionsPanel = DensityBasedConnectionPanel
                .createSparsityAdjustmentPanel((Sparse) connection,
                    networkPanel);
            title += "Sparse";
        }
        ConnectionDialog dialog =
            ConnectionDialog.createConnectionDialog(optionsPanel, connection,
                networkPanel);
        dialog.setTitle(title);
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }

}