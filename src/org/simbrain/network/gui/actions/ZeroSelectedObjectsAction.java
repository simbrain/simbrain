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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.resource.ResourceManager;

/**
 * Clear selected neurons action.
 */
public final class ZeroSelectedObjectsAction extends ConditionallyEnabledAction {

    /**
     * Create a new clear selected neurons action with the specified network
     * panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZeroSelectedObjectsAction(final NetworkPanel networkPanel) {
        super(networkPanel, "Set selected objects to zero", EnablingCondition.ALLITEMS);

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
        putValue(SHORT_DESCRIPTION,
                "Set selected neurons and synapses to zero (c)");
        networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke('c'), this);
        networkPanel.getActionMap().put(this, this);
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (NeuronNode node : networkPanel.getSelectedNeurons()) {
            node.getNeuron().clear();
        }
        for (SynapseNode node : networkPanel.getSelectedSynapses()) {
            node.getSynapse().forceSetStrength(0);
        }
        networkPanel.getNetwork().fireNetworkChanged();
    }
}