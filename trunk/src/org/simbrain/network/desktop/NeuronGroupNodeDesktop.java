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

import org.simbrain.network.gui.nodes.groupNodes.NeuronGroupNode;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.CouplingMenuConsumer;
import org.simbrain.workspace.gui.CouplingMenuProducer;

/**
 * Extends NeuronGroupNode and adds a coupling menu.
 *
 * @author Jeff Yoshimi
 */
public class NeuronGroupNodeDesktop extends NeuronGroupNode {

    /** Reference to workspace component. */
    private final WorkspaceComponent component;

    /**
     * Construct the special node.
     *
     * @param component workspace component reference
     * @param node reference to NeuronGroupNode being wrapped
     */
    public NeuronGroupNodeDesktop(final WorkspaceComponent component,
            final NeuronGroupNode node) {
        super(node.getNetworkPanel(), node.getNeuronGroup());
        this.component = component;
        // TODO: Confusing design...
        //  A subclass of NeuronGroupNode comes in, but is converted in to this
        //  different subclass at the same level.   The main thing lost in the process
        //  is a custom menu, which is passed along here.
        this.setCustomMenu(node.getCustomMenu());
    }

    @Override
    public JMenu getProducerMenu() {
        if (component != null) {
            PotentialProducer producer = component.getAttributeManager()
                    .createPotentialProducer(getGroup(),
                            "getActivations", double[].class);
            producer.setCustomDescription("Neuron Group: "
                    + getGroup().getLabel());
            JMenu producerMenu = new CouplingMenuProducer(
                    "Send Vector Coupling to", component
                            .getWorkspace(), producer);
            return producerMenu;
        }
        return null;

    }


    @Override
    public JMenu getConsumerMenu() {
        if (component != null) {
            PotentialConsumer consumer = component.getAttributeManager()
                    .createPotentialConsumer(getGroup(), "setActivations",
                            double[].class);
            consumer.setCustomDescription("Neuron Group: "
                    + getGroup().getLabel());
            JMenu menu = new CouplingMenuConsumer(
                    "Receive Vector Coupling from", component.getWorkspace(),
                    consumer);
            return menu;
        }
        return null;
    }

}
