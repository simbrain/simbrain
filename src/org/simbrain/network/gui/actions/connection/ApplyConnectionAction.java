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

import org.simbrain.network.connections.*;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.ConditionallyEnabledAction;
import org.simbrain.network.gui.dialogs.connect.ConnectionDialog;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Apply specified connection from source to target neurons.
 */
public final class ApplyConnectionAction extends ConditionallyEnabledAction {

    /**
     * The connection to apply.
     */
    private ConnectNeurons connection;

    /**
     * Connection panel for dialog.
     */
    private AnnotatedPropertyEditor connectionPanel = null;

    /**
     * Construct the action.
     *
     * @param networkPanel networkPanel, must not be null
     * @param connection   the connection to apply
     * @param name         the name of this action
     */
    public ApplyConnectionAction(final NetworkPanel networkPanel, ConnectNeurons connection, String name) {

        super(networkPanel, name, EnablingCondition.SOURCE_AND_TARGET_NEURONS);
        putValue(SHORT_DESCRIPTION, "Use " + name + " method to connect source to target neurons");
        this.connection = connection;

    }

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(final ActionEvent event) {

        String title = "Connect " + connection.toString();

        if (connection instanceof RadialGaussian) {
            connectionPanel = new AnnotatedPropertyEditor((RadialGaussian) connection);
        } else if (connection instanceof AllToAll) {
            connectionPanel = new AnnotatedPropertyEditor((AllToAll) connection);
        } else if (connection instanceof OneToOne) {
            connectionPanel = new AnnotatedPropertyEditor((OneToOne) connection);
        }  else if (connection instanceof Sparse) {
            connectionPanel = new AnnotatedPropertyEditor((Sparse) connection);
        }  else if (connection instanceof RadialSimple) {
            connectionPanel = new AnnotatedPropertyEditor((RadialSimple) connection);
        }

        ConnectionDialog dialog = new ConnectionDialog(networkPanel, connection);
        dialog.setTitle(title);
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }

}