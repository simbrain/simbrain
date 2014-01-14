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
import org.simbrain.network.gui.nodes.SynapseGroupNode;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.CouplingMenuConsumer;
import org.simbrain.workspace.gui.CouplingMenuProducer;

/**
 * Extends SynapseGroupNode and adds a coupling menu.
 *
 * TODO: workspace couplings for group nodes not yet working properly. Once
 * they are it may be possible to remove this class.
 *
 * @author Jeff Yoshimi
 */
public class SynapseGroupNodeDesktop extends SynapseGroupNode {

    /** Reference to workspace component. */
    private final WorkspaceComponent component;

    /**
     * Construct the special node.
     *
     * @param component workspace component reference
     * @param networkPanel network panel reference
     * @param group the synapse group being represented by this node
     */
    public SynapseGroupNodeDesktop(WorkspaceComponent component,
            NetworkPanel networkPanel, SynapseGroup group) {
        super(networkPanel, group);
        this.component = component;
    }

    public JMenu getProducerMenu() {
        if (component != null) {
            PotentialProducer producer = component.getAttributeManager()
                    .createPotentialProducer(getSynapseGroup(), "getWeightVector",
                            double[].class);
            producer.setCustomDescription("Synapse Group: "
                    + getSynapseGroup().getLabel());
            JMenu producerMenu = new CouplingMenuProducer(
                    "Send Vector Coupling to", component.getWorkspace(),
                    producer);
            return producerMenu;
        }
        return null;
    }

    public JMenu getConsumerMenu() {
        if (component != null) {
            PotentialConsumer consumer = component.getAttributeManager()
                    .createPotentialConsumer(getSynapseGroup(), "setWeightVector",
                            double[].class);
            consumer.setCustomDescription("Synapse Group: "
                    + getSynapseGroup().getLabel());

            JMenu menu = new CouplingMenuConsumer(
                    "Receive Vector Coupling from", component.getWorkspace(),
                    consumer);
            return menu;
        }
        return null;
    }

}
