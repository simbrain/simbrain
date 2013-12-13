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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.NetworkPanel;

/**
 * <b>SparsePanel</b> creates a dialog for setting preferences of Sparse neuron
 * connections.
 *
 * @author ztosi
 *
 */
public class SparsePanel extends AbstractConnectionPanel {

    /**
     * A sub-panel for editing the connection sparsity.
     */
    private final SparsityAdjustmentPanel saPanel;

    /**
     * A sub-panel for editing the ratio of excitatory to inhibitory connecitons
     */
    private final ExcitatoryInhibitoryRatioPanel eirPanel;

    /**
     * A sub-panel for editing the synapse type and the weight distribution.
     */
    private final SynapsePropertiesPanel spPanel;

    /** A check box for determining if self-connections are to be allowed. */
    private JCheckBox allowSelfConnect = new JCheckBox();

    /** The number of target neurons. */
    private final int numTargs;

    /**
     * This method is the default constructor.
     *
     * @param connection type
     */
    public SparsePanel(final Sparse connection, final NetworkPanel networkPanel) {
        super(connection);
        numTargs = networkPanel.getSelectedModelNeurons().size();
        eirPanel = new ExcitatoryInhibitoryRatioPanel(connection);
        saPanel = new SparsityAdjustmentPanel(connection, numTargs);
        spPanel = new SynapsePropertiesPanel(connection);

        fillFieldValues();
        initializeLayout();

    }

    /**
     * Initializes the custom layout of the sparse panel.
     */
    private void initializeLayout() {

        JPanel sparseContainer = new JPanel(new GridBagLayout());
        this.add(sparseContainer);
        JPanel ei = new JPanel(new BorderLayout());

        // A sub-sub-panel for the "allow self connections" checkbox and label
        JPanel allowSelfConnectPanel = new JPanel(new FlowLayout());
        JLabel selfConnectLabel = new JLabel("Self-Connections: ");
        Font font = selfConnectLabel.getFont();
        Font bold = new Font(font.getName(), Font.BOLD, font.getSize());
        selfConnectLabel.setFont(bold);
        allowSelfConnectPanel.add(selfConnectLabel);
        allowSelfConnectPanel.add(allowSelfConnect);

        /*
         * Add a listener to allowSelfConnect allowing the sparsity adjustment
         * sub-panel to change its field values accordingly.
         */
        allowSelfConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                saPanel.setAllowSelfConnect(allowSelfConnect.isSelected());
            }
        });

        Border l = BorderFactory.createEtchedBorder();
        Border sparseBorder = BorderFactory.createTitledBorder(l, "Sparsity");
        saPanel.setBorder(sparseBorder);
        sparseBorder = BorderFactory.createTitledBorder(l,
                "Excitatory/Inhibitory Properties");
        ei.setBorder(sparseBorder);
        ei.add(eirPanel, BorderLayout.NORTH);
        ei.add(allowSelfConnectPanel, BorderLayout.CENTER);
        ei.add(spPanel, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 0, 5);
        sparseContainer.add(saPanel, gbc);

        gbc.gridy++;
        sparseContainer.add(ei, gbc);

    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        saPanel.commitChanges();
        eirPanel.commitChanges();
        spPanel.commitChanges();
        ((Sparse) connection).setAllowSelfConnection(allowSelfConnect
                .isSelected());
    }

    /**
     * {@inheritDoc}
     *
     * Performed in the sub-panel constructors. TODO: Deprecate?
     */
    public void fillFieldValues() {
    }

    /**
     * Fills the field values of this panel based on an already existing connect
     * neurons object.
     */
    public void fillFieldValues(ConnectNeurons connection) {
        this.connection = connection;
        try {
            saPanel.fillFieldValues((Sparse) connection);
        } catch (ClassCastException e) {
            // TODO: How to handle this case...
        }
        spPanel.fillFieldValues(connection);
        eirPanel.fillFieldValues(connection);
    }

    /**
     * Returns the connection object associated with this panel.
     */
    public Sparse getConnection() {
        return (Sparse) connection;
    }

}
