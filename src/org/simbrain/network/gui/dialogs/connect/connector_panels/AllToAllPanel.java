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
package org.simbrain.network.gui.dialogs.connect.connector_panels;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.AbstractConnectionPanel;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * The <b>AllToAllPanel</b> is panel for editing all to all connections.
 *
 * @author Zoë Tosi
 */
@SuppressWarnings("serial")
public class AllToAllPanel extends AbstractConnectionPanel {

    /**
     * A check box for determining whether or not self connections are allowed.
     */
    private JCheckBox allowSelfConnectChkBx = new JCheckBox();

    /**
     * Panel holding the checkbox.
     */
    private JPanel allowSelfConnectPanel = new JPanel();

    /**
     * The all to all connection object to which changes to this panel will be
     * committed.
     */
    private AllToAll connection;

    /**
     * Construct a new all to all panel.
     *
     * @param connector    the connection object
     * @param networkPanel the parent network panel
     */
    public AllToAllPanel(AllToAll connector, NetworkPanel networkPanel) {
        super();
        this.connection = connector;
        allowSelfConnectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        allowSelfConnectPanel.add(new JLabel(" Allow Self Connections: "));
        allowSelfConnectPanel.add(allowSelfConnectChkBx);
        fillFieldValues();
        add(allowSelfConnectPanel);
    }

    @Override
    public void fillFieldValues() {
        allowSelfConnectChkBx.setSelected(((AllToAll) connection).isSelfConnectionAllowed());
    }

    @Override
    public boolean commitChanges() {
        connection.setSelfConnectionAllowed(allowSelfConnectChkBx.isSelected());
        return true;
    }

    @Override
    public List<Synapse> applyConnection(List<Neuron> source, List<Neuron> target) {
        return AllToAll.connectAllToAll(source, target, Utils.intersects(source, target), connection.isSelfConnectionAllowed(), true);
    }

    @Override
    public ConnectNeurons getConnection() {
        return connection;
    }

}
