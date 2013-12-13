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

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;

/**
 * <b>AllToAllPanel</b> creates displays preferences for all to all neuron
 * connections.
 *
 * @author ztosi
 * @author jyoshimi
 *
 */
public class AllToAllPanel extends AbstractConnectionPanel {

    /** The panel for setting the ratio of excitatory to inhibitory synapses. */
    private ExcitatoryInhibitoryRatioPanel eirPanel;

    /** The panel for setting learning rules and randomization for synapses. */
    private SynapsePropertiesPanel spPanel;

    /** Allow self connection check box. */
    private JCheckBox allowSelfConnect = new JCheckBox();

    /**
     * This method is the default constructor.
     *
     * @param connection type
     */
    public AllToAllPanel(final AllToAll connection) {
        super(connection);
        eirPanel = new ExcitatoryInhibitoryRatioPanel(connection);
        spPanel = new SynapsePropertiesPanel(connection);
        initializeLayout();
    }

    private void initializeLayout() {

        // A sub-panel for the synapse properties and ei panel
        JPanel pPanel = new JPanel(new BorderLayout());
        // A sub-sub-panel for the "allow self connections" checkbox and label
        JPanel allowSelfConnectPanel = new JPanel(new FlowLayout());
        JLabel selfConnectLabel = new JLabel("Self-Connections: ");
        Font font = selfConnectLabel.getFont();
        Font bold = new Font(font.getName(), Font.BOLD, font.getSize());
        selfConnectLabel.setFont(bold);
        allowSelfConnectPanel.add(selfConnectLabel);
        allowSelfConnectPanel.add(allowSelfConnect);

        pPanel.add(eirPanel, BorderLayout.NORTH);
        pPanel.add(allowSelfConnectPanel, BorderLayout.CENTER);
        pPanel.add(spPanel, BorderLayout.SOUTH);
        Border b = BorderFactory.createEtchedBorder();
        Border pBorder = BorderFactory.createTitledBorder(b,
                "Excitatory/Inhibitory Properties");
        pPanel.setBorder(pBorder);

        // Add the first sub-sub panel (eip and synapse properties)
        this.add(pPanel, BorderLayout.CENTER);

    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        eirPanel.commitChanges();
        spPanel.commitChanges();
        ((AllToAll) connection).setAllowSelfConnection(allowSelfConnect
                .isSelected());
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {
    }

    @Override
    public void fillFieldValues(ConnectNeurons connection)
            throws ClassCastException {
        eirPanel.fillFieldValues(connection);
        spPanel.fillFieldValues(connection);
    }

}
