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

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.AllToAllPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.SparseConnectionPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.OneToOnePanel;

/**
 * Panel for editing connection objects when creating new synapse groups.
 *
 * Very similar to {@link QuickConnectPreferencesPanel} (with some repeated
 * code). Perhaps factor out common code later.
 */
public final class ConnectionPanel {

    /** Parent frame so pack can be called when combo box changed. */
    private final Window parentFrame;

    /** Parent network panel. */
    private final NetworkPanel networkPanel;

    /** The main panel holding the connection panels. */
    private final JPanel mainPanel;

    /** The all to all connector. */
    private final AllToAll allToAll = new AllToAll();

    /** The one to one connector. */
    private final OneToOne oneToOne = new OneToOne();

    /** The sparse connector. */
    private final Sparse sparse = new Sparse();

    /** Select the connection type. */
    private JComboBox<ConnectNeurons> cbConnectionType = new JComboBox<ConnectNeurons>(
            new ConnectNeurons[] { allToAll, oneToOne, sparse });

    /** Panel with card layout showing the different connection panels. */
    private JPanel connectPanel = new JPanel();

    /** List of connection panels used to separately set their preferred sizes. */
    private AbstractConnectionPanel[] connectorPanels = new AbstractConnectionPanel[3];

    /**
     * Construct a connection panel.
     *
     * @param parent parent window
     * @param networkPanel parent network panel
     * @return constructed connection panel
     */
    public static ConnectionPanel createConnectionPanel(final Window parent,
            final NetworkPanel networkPanel) {
        ConnectionPanel cp = new ConnectionPanel(parent, networkPanel);
        cp.addListeners();
        return cp;
    }

    /**
     * Private constructor for the factory method.
     *
     * @param parent parent window
     * @param networkPanel parent network panel
     */
    private ConnectionPanel(final Window parent, final NetworkPanel networkPanel) {
        this.parentFrame = parent;
        this.networkPanel = networkPanel;
        mainPanel = new JPanel();
        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {
        connectPanel.setLayout(new CardLayout());
        connectorPanels[0] = new AllToAllPanel(allToAll, networkPanel);
        connectorPanels[1] = new OneToOnePanel(oneToOne);
        connectorPanels[2] = SparseConnectionPanel
                .createSparsityAdjustmentPanel(sparse, networkPanel);
        connectPanel.add(connectorPanels[0], AllToAll.getName());
        connectPanel.add(connectorPanels[1], OneToOne.getName());
        connectPanel.add(connectorPanels[2], Sparse.getName());
        ((CardLayout) connectPanel.getLayout()).show(connectPanel,
                AllToAll.getName());

        JPanel cbPanel = new JPanel(new FlowLayout());
        cbPanel.add(new JLabel("Connection Manager: "));
        cbPanel.add(cbConnectionType);

        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(cbPanel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        mainPanel.add(connectPanel, gbc);

    }

    /**
     * Respond to changes in the combo box.
     */
    private void addListeners() {
        cbConnectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                CardLayout cl = (CardLayout) connectPanel.getLayout();
                cl.show(connectPanel, cbConnectionType.getSelectedItem()
                        .toString());
                connectPanel.setPreferredSize(getSelectedPanel()
                        .getPreferredSize());
                mainPanel.revalidate();
                mainPanel.repaint();
                parentFrame.pack();
            }
        });
    }

    /**
     * Helper method to get the current card panel.
     *
     * @return the currently visible panel
     */
    private AbstractConnectionPanel getSelectedPanel() {
        for (AbstractConnectionPanel p : connectorPanels) {
            if (p.isVisible()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Use the state of the gui to set the connection object of the synapse
     * group being created.
     *
     * @param synapseGroup the synapse group whose connection object is to be
     *            set
     */
    public void commitChanges(SynapseGroup synapseGroup) {
        AbstractConnectionPanel acp = getSelectedPanel();
        acp.commitChanges();
        synapseGroup.setConnectionManager(acp.getConnection());
    }

    /**
     * @return the mainPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Test the connection panel.
     * @param args 
     */
    public static void main(String[] args) {
        NetworkPanel np = new NetworkPanel(new Network());
        JFrame frame = new JFrame();
        ConnectionPanel cp = createConnectionPanel(frame, np);
        frame.setContentPane(cp.mainPanel);
        cp.mainPanel.setVisible(true);
        frame.setVisible(true);
        frame.pack();
    }

}
