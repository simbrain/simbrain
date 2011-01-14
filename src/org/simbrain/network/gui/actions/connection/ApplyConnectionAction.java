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
package org.simbrain.network.gui.actions.connection;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.NetworkPanel;

/**
 * Apply specified connection either from selected neurons to themselves
 * ("self connect") or form source to target.
 */
public final class ApplyConnectionAction extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** If true, connect selected neurons to themselves. */
    private boolean isSelfConnect;

    /** The connection to apply. */
    private ConnectNeurons connection;

    /**
     * Construct the action.
     *
     * @param networkPanel networkPanel, must not be null
     * @param connection the connection to apply
     * @param name the name of this action
     * @param isSelfConnect whether to connect selected neurons to themselves or
     *            not.
     */
    public ApplyConnectionAction(final NetworkPanel networkPanel,
            ConnectNeurons connection, String name, boolean isSelfConnect) {

        super(name);

        this.isSelfConnect = isSelfConnect;
        this.connection = connection;

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        if (isSelfConnect) {
            connection.connectNeurons(networkPanel.getRootNetwork(),
                    networkPanel.getSelectedModelNeurons(),
                    networkPanel.getSelectedModelNeurons());
        } else {
            connection.connectNeurons(networkPanel.getRootNetwork(),
                    networkPanel.getSourceModelNeurons(),
                    networkPanel.getSelectedModelNeurons());
        }
    }
}