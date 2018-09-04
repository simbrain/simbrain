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
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel for selecting a type of connection.
 * <p>
 * Contains a set of connection manager objects. Returns
 * the current conection manager,  which can then be used to
 * create connections or store connection settings.
 */
public class ConnectionSelectorPanel extends JPanel {

    /**
     * Temporary list of connection panels managed by combo box.
     */
    private final ConnectNeurons[] CONNECTORS = {new AllToAll(), new OneToOne(),
        new RadialSimple(), new RadialGaussian()};

    /**
     * Select connection type.
     */
    private JComboBox<ConnectNeurons> cbConnectionType;

    /**
     * The current seelcted connection panel
     */
    private ConnectionPanel currentConnectionPanel;

    /**
     * Gridbag constraints for this panel's layout.
     */
    private GridBagConstraints gbc;

    /**
     * Connection dialog default constructor.
     */
    public ConnectionSelectorPanel() {
        cbConnectionType = new JComboBox(CONNECTORS);
        init();
    }

    /**
     * Construct the panel using a specific starting connector.
     *
     * @param initConnection initial connection manager
     */
    public ConnectionSelectorPanel(ConnectNeurons initConnection) {
        cbConnectionType = new JComboBox(CONNECTORS);
        for(ConnectNeurons cn : CONNECTORS) {
            if(cn.getClass() == initConnection.getClass()) {
                cbConnectionType.removeItem(cn);
                cbConnectionType.addItem(initConnection);
                // TODO: alphebatize
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
    public ConnectionSelectorPanel(ConnectNeurons[] connectionManagers, ConnectNeurons initConnection) {
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
        gbc.fill = GridBagConstraints.BOTH;

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
        currentConnectionPanel = new ConnectionPanel(null,
            (ConnectNeurons) cbConnectionType.getSelectedItem());
        add(currentConnectionPanel, gbc);
    }

    /**
     * Get the current selected connection manager.
     */
    public ConnectNeurons getSelectedConnector() {
        return currentConnectionPanel.getConnection();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        currentConnectionPanel.commitSettings();
    }

}
