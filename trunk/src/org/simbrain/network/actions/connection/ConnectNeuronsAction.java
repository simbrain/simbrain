/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.actions.connection;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simnet.NetworkPreferences;
import org.simnet.connections.AllToAll;
import org.simnet.connections.ConnectNeurons;
import org.simnet.connections.OneToOne;
import org.simnet.connections.Sparse;

/**
 * Connect neurons action.  Connects a set of source neurons to a set of target neurons.
 */
public final class ConnectNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Source neuron. */
    private ArrayList sourceNeurons;

    /** Target neuron. */
    private ArrayList targetNeurons;


    /**
     * Create a new connect neurons action.  Connects a set of source neurons to a set of target neurons.
     *
     * @param networkPanel network panel, must not be null
     * @param sourceNeurons NeuronNodes to connect from
     * @param targetNeurons NeuronNodes to connect to
     */
    public ConnectNeuronsAction(final NetworkPanel networkPanel,
                                final ArrayList sourceNeurons,
                                final ArrayList targetNeurons) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.sourceNeurons = sourceNeurons;
        this.targetNeurons = targetNeurons;

        putValue(NAME, "Connect using \"" + NetworkPreferences.getConnectionType() + "\"");

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        if (sourceNeurons.isEmpty() || targetNeurons.isEmpty()) {
            return;
        }
        ConnectNeurons connection;
        if (NetworkPreferences.getConnectionType().equals("All to All")) {
            connection = new AllToAll(networkPanel.getRootNetwork(), sourceNeurons, targetNeurons);
        } else if (NetworkPreferences.getConnectionType().equals("One to One")) {
            connection = new OneToOne(networkPanel.getRootNetwork(), sourceNeurons, targetNeurons);
        } else if (NetworkPreferences.getConnectionType().equals("Sparse")) {
            connection = new Sparse(networkPanel.getRootNetwork(), sourceNeurons, targetNeurons);
        } else {
            System.out.println("Conditions Failed");
            return;
        }
        connection.connectNeurons();
    }
}