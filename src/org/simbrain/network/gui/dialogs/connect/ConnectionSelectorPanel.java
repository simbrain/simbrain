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

import org.simbrain.network.connections.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Panel for selecting a type of connection.
 * <p>
 * Contains a set of connection manager objects. Returns
 * the current conection manager,  which can then be used to
 * create connections or store connection settings.
 */
public class ConnectionSelectorPanel extends JPanel {

    private final Window parentFrame;

    /**
     * Temporary list of connection panels managed by combo box.
     */
    private final ConnectionStrategy[] CONNECTORS = {new AllToAll(), new OneToOne(),
        new RadialSimple(), new RadialGaussian(), new Sparse()};

    /**
     * Select connection type.
     */
    private JComboBox<ConnectionStrategy> cbConnectionType;

    /**
     * The current selected connection panel
     */
    private ConnectionPanel currentConnectionPanel;

    /**
     * Gridbag constraints for this panel's layout.
     */
    private GridBagConstraints gbc;

    /**
     * Whether or not the connections that are/will be made are
     * recurrent.
     */
    private boolean recurrent;

    /**
     * Connection dialog default constructor.
     */
    public ConnectionSelectorPanel(boolean recurrent, Window parentFrame) {
        this.parentFrame=parentFrame;
        cbConnectionType = new JComboBox(CONNECTORS);
        this.recurrent = recurrent;
        init();
    }

    /**
     * Construct the panel using a specific starting connector.
     *
     * @param initConnection initial connection manager
     */
    public ConnectionSelectorPanel(ConnectionStrategy initConnection, Window parentFrame) {
        this.parentFrame=parentFrame;
        cbConnectionType = new JComboBox(CONNECTORS);
        for(ConnectionStrategy cn : CONNECTORS) {
            if(cn.getClass() == initConnection.getClass()) {
                cbConnectionType.removeItem(cn);
                cbConnectionType.addItem(initConnection);
                break;
            }
        }
        cbConnectionType.setSelectedItem(initConnection);
        init();
    }

    /**
     * Construct the panel from a specified set of connection manager objects.
     *
     * @param connectionManagers list of connection managers for drop down
     * @param initConnection initial connection manager
     */
    public ConnectionSelectorPanel(ConnectionStrategy[] connectionManagers,
                                   ConnectionStrategy initConnection, Window parentFrame) {
        this.parentFrame = parentFrame;
        cbConnectionType = new JComboBox(connectionManagers);
        cbConnectionType.setSelectedItem(initConnection);
        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {


        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 15, 30, 5);
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Connection Manager:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 30, 5);
        gbc.fill = GridBagConstraints.NONE;
        add(cbConnectionType, gbc);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        updatePanel();

        cbConnectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updatePanel();
            }
        });
    }

    /**
     * Update the panel when the combo box is changed.
     */
    private void updatePanel() {
        if (currentConnectionPanel != null) {
            remove(currentConnectionPanel);
        }
        int noTar = 0;
        if(cbConnectionType.getSelectedItem().getClass() == Sparse.class) {
            if(((Sparse) cbConnectionType.getSelectedItem()).getSynapseGroup() != null) {
                noTar = ((Sparse) cbConnectionType.getSelectedItem())
                        .getSynapseGroup().getTargetNeuronGroup()
                        .size();
            }
        }
        currentConnectionPanel = new ConnectionPanel(parentFrame,
            (ConnectionStrategy) cbConnectionType.getSelectedItem(),
                noTar, recurrent);
        add(currentConnectionPanel, gbc);
        repaint();
        parentFrame.pack();
    }

    /**
     * Get the current selected connection manager.
     */
    public ConnectionStrategy getSelectedConnector() {
        return currentConnectionPanel.getConnectionStrategy();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        currentConnectionPanel.commitSettings();
    }

}
