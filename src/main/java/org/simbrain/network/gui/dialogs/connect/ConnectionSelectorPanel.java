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
import org.simbrain.util.widgets.EditablePanel;

import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

/**
 * Panel for selecting a type of connection.
 * <p>
 * Contains a set of connection manager objects. Returns
 * the current conection manager,  which can then be used to
 * create connections or store connection settings.
 */
public class ConnectionSelectorPanel extends EditablePanel {

    /**
     * So that it can be resized on updates with pack().
     */
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
     * Whether or not this is a panel that is a part of creating a new synapse group or editing an extant one.
     */
    private boolean isCreation;

    /**
     * Connection dialog default constructor.
     */
    public ConnectionSelectorPanel(boolean recurrent, Window parentFrame, boolean isCreation) {
        this.parentFrame = parentFrame;
        cbConnectionType = new JComboBox(CONNECTORS);
        this.recurrent = recurrent;
        this.isCreation = isCreation;
        init();
    }

    /**
     * Construct the panel using a specific starting connector.
     *
     * @param initConnection initial connection manager
     */
    public ConnectionSelectorPanel(ConnectionStrategy initConnection, Window parentFrame, boolean isCreation) {
        this.parentFrame = parentFrame;
        cbConnectionType = new JComboBox(CONNECTORS);
        cbConnectionType.setPreferredSize(new Dimension(200, 20));
        this.isCreation = isCreation;
        setComboBox(initConnection);
        init();
    }

    /**
     * Sets selected item in the combo box to the indicated strategy
     */
    private void setComboBox(ConnectionStrategy strategy) {
        for(ConnectionStrategy cs : CONNECTORS) {
            if (cs.getName().equals(strategy.getName())) {
                // TODO: Must copy strategy over to cs
                cbConnectionType.setSelectedItem(cs);
            }
        }
    }

    /**
     * Initialize the panel.
     */
    private void init() {

        // Combo Box
        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(20, 15, 15, 5);
        add(new JLabel("Connection Manager:"), gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(20, 5, 15, 5);
        add(cbConnectionType, gbc);
        cbConnectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updatePanel();
            }
        });

        // Main panel
        updatePanel();

    }

    /**
     * Update the panel when the combo box is changed.
     */
    private void updatePanel() {
        if (currentConnectionPanel != null) {
            remove(currentConnectionPanel);
        }
        int noTar = 0;
        if (cbConnectionType.getSelectedItem().getClass() == Sparse.class) {
            if (((Sparse) cbConnectionType.getSelectedItem()).getSynapseGroup() != null) {
                noTar = ((Sparse) cbConnectionType.getSelectedItem())
                        .getSynapseGroup().getTargetNeuronGroup()
                        .size();
            }
        }
        currentConnectionPanel = new ConnectionPanel(parentFrame,
                (ConnectionStrategy) cbConnectionType.getSelectedItem(),
                noTar, recurrent, isCreation);

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = .8;
        gbc.gridwidth = 2; // to span the label and the combo box
        gbc.insets = new Insets(0, 5, 0, 5);
        // Useful for debugging
        //currentConnectionPanel.setBorder(new LineBorder(Color.red));
        add(currentConnectionPanel, gbc);
        repaint();
        parentFrame.pack();
        parentFrame.setLocationRelativeTo(null);
    }

    /**
     * Get the current selected connection manager.
     */
    public ConnectionStrategy getSelectedConnector() {
        return currentConnectionPanel.getConnectionStrategy();
    }

    @Override
    public void fillFieldValues() {
    }

    @Override
    public boolean commitChanges() {
        currentConnectionPanel.commitSettings();
        return true;
    }

    public ConnectionPanel getCurrentConnectionPanel() {
        return currentConnectionPanel;
    }
}
