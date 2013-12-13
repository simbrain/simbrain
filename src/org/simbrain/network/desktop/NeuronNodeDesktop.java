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
package org.simbrain.network.desktop;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.CouplingMenuConsumer;
import org.simbrain.workspace.gui.CouplingMenuProducer;

/**
 * Version of a Neuron Node with a coupling menu.
 */
public class NeuronNodeDesktop extends NeuronNode {

    /** Reference to parent component. */
    private NetworkComponent component;

    /**
     * Constructs a Neuron Node.
     *
     * @param component parent component.
     * @param netPanel network panel.
     * @param neuron logical neuron this node represents
     */
    public NeuronNodeDesktop(final NetworkComponent component,
            final NetworkPanel netPanel, Neuron neuron) {
        super(netPanel, neuron);
        this.component = component;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();

        // Add coupling menus
        Workspace workspace = component.getWorkspace();
        if (getNetworkPanel().getSelectedNeurons().size() == 1) {
            contextMenu.addSeparator();
            PotentialProducer producer = component.getAttributeManager()
                    .createPotentialProducer(neuron, "getActivation",
                            double.class);
            PotentialConsumer consumer = component.getAttributeManager()
                    .createPotentialConsumer(neuron, "setInputValue",
                            double.class);
            JMenu producerMenu = new CouplingMenuProducer(
                    "Send Scalar Coupling to", workspace, producer);
            contextMenu.add(producerMenu);
            JMenu consumerMenu = new CouplingMenuConsumer(
                    "Receive Scalar Coupling from", workspace, consumer);
            contextMenu.add(consumerMenu);
        }
        return contextMenu;
    }

}
