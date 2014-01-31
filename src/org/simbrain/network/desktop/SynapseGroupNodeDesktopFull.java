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

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.SynapseGroupNodeFull;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.CouplingMenuConsumer;
import org.simbrain.workspace.gui.CouplingMenuProducer;

/**
 * Extends SynapseGroupNode and adds a coupling menu.
 *
 * @author Jeff Yoshimi
 */
public class SynapseGroupNodeDesktopFull extends SynapseGroupNodeFull {

    /** Reference to workspace component. */
    private final WorkspaceComponent component;

    /**
     * Construct the special node.
     *
     * @param component workspace component reference
     * @param networkPanel network panel reference
     * @param group the synapse group being represented by this node
     */
    public SynapseGroupNodeDesktopFull(WorkspaceComponent component,
            NetworkPanel networkPanel, SynapseGroup group) {
        super(networkPanel, group);
        this.component = component;
    }

    @Override
    public JMenu getProducerMenu() {
        return getProducerMenu(component, synapseGroup);
    }

    @Override
    public JMenu getConsumerMenu() {
        return getConsumerMenu(component, synapseGroup);
    }

    /**
     * Creates a producer menu for a synapse group. Static method so that
     * SynapseGroupNodeSimple can also use this.
     *
     * @param theComponent workspace component references
     * @param sg the synapse group
     * @return the decorated menu
     */
    static JMenu getProducerMenu(WorkspaceComponent theComponent,
            SynapseGroup sg) {
        if (theComponent != null) {
            PotentialProducer producer = theComponent.getAttributeManager()
                    .createPotentialProducer(sg, "getWeightVector",
                            double[].class);
            producer.setCustomDescription("Synapse Group: " + sg.getLabel());
            JMenu producerMenu = new CouplingMenuProducer(
                    "Send Vector Coupling to", theComponent.getWorkspace(),
                    producer);
            return producerMenu;
        }
        return null;
    }

    /**
     * Creates a consumer menu for a synapse group. Static method so that
     * SynapseGroupNodeSimple can also use this.
     *
     * @param theComponent workspace component references
     * @param sg the synapse group
     * @return the decorated menu
     */
    static JMenu getConsumerMenu(WorkspaceComponent theComponent,
            SynapseGroup sg) {
        if (theComponent != null) {
            PotentialConsumer consumer = theComponent.getAttributeManager()
                    .createPotentialConsumer(sg, "setWeightVector",
                            double[].class);
            consumer.setCustomDescription("Synapse Group: " + sg.getLabel());

            JMenu menu = new CouplingMenuConsumer(
                    "Receive Vector Coupling from",
                    theComponent.getWorkspace(), consumer);
            return menu;
        }
        return null;
    }

}
