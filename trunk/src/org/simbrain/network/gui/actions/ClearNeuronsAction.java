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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.resource.ResourceManager;

/**
 * Clear selected neurons action.
 */
public final class ClearNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clear selected neurons action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public ClearNeuronsAction(final NetworkPanel networkPanel) {
        super("Clear selected neurons");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
        putValue(SHORT_DESCRIPTION, "Zero selected Nodes (c)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('c'), this);
        networkPanel.getActionMap().put(this, this);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (Iterator i = networkPanel.getSelectedNeurons().iterator(); i.hasNext(); ) {
            NeuronNode node = (NeuronNode) i.next();
            node.getNeuron().clear();
        }
        for (Iterator i = networkPanel.getSelectedSynapses().iterator(); i.hasNext(); ) {
            SynapseNode node = (SynapseNode) i.next();
            node.getSynapse().setStrength(0);
        }
        networkPanel.getRootNetwork().fireNetworkChanged();
    }
}