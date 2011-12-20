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
package org.simbrain.network.gui.dialogs.connect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.interfaces.Synapse;

/**
 * <b>OneToOnePanel</b> creates a dialog for setting preferences of one to one
 * neuron connections.
 */
public class OneToOnePanel extends AbstractConnectionPanel {

    /** Label showing current type of synapse. */
    private JLabel baseSynapseLabel = new JLabel("");

    /** Sets the connection orientation. */
    private JComboBox orientationBox;

    /** Sets whether connections are bidirectional. */
    private JCheckBox bidirectionalConnection = new JCheckBox();

    /**
     * Default constructor.
     */
    public OneToOnePanel(final OneToOne connection) {
        super(connection);
        orientationBox = new JComboBox(OneToOne.getOrientationTypes());
        JButton setSynapseType = new JButton("Set...");
        setSynapseType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> list = new ArrayList<Synapse>();
                Synapse temp = connection.getBaseSynapse();
                list.add(temp);
                SynapseDialog dialog = new SynapseDialog(list);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse synapse = dialog.getSynapseList().get(0);
                connection.setBaseSynapse(synapse);
                baseSynapseLabel.setText(synapse.getType());
            }

        });
        baseSynapseLabel.setText(connection.getBaseSynapse().getType());
        this.addItem("Base Synapse Type:", baseSynapseLabel);
        this.addItem("Set Base Synapse Type:", setSynapseType);
        this.addItem("Connection Orientation: ", orientationBox);
        this.addItem("Bidirectional Connection: ", bidirectionalConnection);
    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        ((OneToOne) connection)
                .setUseBidirectionalConnections(bidirectionalConnection
                        .isSelected());
        ((OneToOne) connection)
                .setConnectOrientation((Comparator) orientationBox
                        .getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {
        bidirectionalConnection.setSelected(((OneToOne) connection)
                .isUseBidirectionalConnections());
        orientationBox.setSelectedItem(((OneToOne) connection)
                .getConnectOrientation());
    }

}
