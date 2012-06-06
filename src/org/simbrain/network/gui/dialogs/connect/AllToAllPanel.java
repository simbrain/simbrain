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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.simbrain.network.connections.AllToAll;

/**
 * <b>AllToAllPanel</b> creates displays preferences for all to all neuron
 * connections.
 *
 * @author ztosi
 * @author jyoshimi
 *
 */
public class AllToAllPanel extends AbstractConnectionPanel {

    private ExcitatoryInhibitoryPropertiesPanel eipPanel;

    /** Allow self connection check box. */
    private JCheckBox allowSelfConnect = new JCheckBox();

    /**
     * This method is the default constructor.
     *
     * @param connection type
     */
    public AllToAllPanel(final AllToAll connection) {
        super(connection);
        eipPanel = new ExcitatoryInhibitoryPropertiesPanel(connection);
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 9;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        this.add(eipPanel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 0, 10);
        gbc.gridy = 9;
        gbc.gridheight = 1;
        this.add(new JSeparator(), gbc);

        gbc.gridy = 10;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 0, 10);

        JPanel allowSelfConnectPanel = new JPanel();
        FlowLayout ASCPFL = new FlowLayout(FlowLayout.LEFT);
        allowSelfConnectPanel.setLayout(ASCPFL);
        allowSelfConnectPanel.add(new JLabel("Allow Self-Connections: "));
        allowSelfConnectPanel.add(allowSelfConnect);

        this.add(allowSelfConnectPanel, gbc);
    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        ((AllToAll) connection).setAllowSelfConnection(allowSelfConnect
                .isSelected());
        eipPanel.commitChanges();
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {

    }

}
