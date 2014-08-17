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
package org.simbrain.network.gui.dialogs.connect.connector_panels;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.dialogs.connect.AbstractConnectionPanel;

/**
 * <b>OneToOnePanel</b> creates a dialog for setting preferences of one to one
 * neuron connections.
 */
@SuppressWarnings("serial")
public class OneToOnePanel extends AbstractConnectionPanel {

    public enum OneToOneOptions {
        HORIZONTAL, VERTICAL, AUTO_DETECT;
    }

    /** Sets the connection orientation. */
    private JComboBox orientationBox;

    /** Sets whether connections are bidirectional. */
    private JCheckBox bidirectionalConnection = new JCheckBox();

    /** The connection object to be edited. */
    private OneToOne connection;

    /**
     * Default constructor.
     */
    public OneToOnePanel(final OneToOne connection) {
        super();
        this.connection = connection;
        orientationBox = new JComboBox(OneToOne.getOrientationTypes());
        // setSynapseType.setText(connection.getBaseSynapse().getType());
        //        mainPanel.addItem("Orientation: ", orientationBox);
        mainPanel.addItem("Bidirectional Connections: ",
            bidirectionalConnection);
        fillFieldValues();
    }

    @Override
    public void fillFieldValues() {
        bidirectionalConnection.setSelected(((OneToOne) connection)
            .isUseBidirectionalConnections());
        orientationBox.setSelectedItem(((OneToOne) connection)
            .getConnectOrientation());
    }

    @Override
    public OneToOne getConnection() {
        return connection;
    }

    @Override
    public boolean commitChanges() {
        connection.setUseBidirectionalConnections(bidirectionalConnection
            .isSelected());
        //        connection
        //            .setConnectOrientation(((OrientationComparator) orientationBox
        //                .getSelectedItem()));
        return true;

    }

    @Override
    public List<Synapse> applyConnection(List<Neuron> source,
        List<Neuron> target) {
        return OneToOne.connectOneToOne(source, target,
            bidirectionalConnection.isSelected(), true);
    }

}
