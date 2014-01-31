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

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronGroupNode;
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

    /** Reference to the neuron group being represented. */
    private final NeuronGroup neuronGroup;

    /**
     * Construct the desktop version of the neuron group node.
     *
     * @param component workspace component reference
     * @param panel parent network panel
     * @param node reference to neuronGroupNode being decorated
     */
    public NeuronGroupNodeDesktop(final WorkspaceComponent component,
            final NetworkPanel panel, final NeuronGroupNode node) {
        super(panel, node.getNeuronGroup());
        this.neuronGroup = node.getNeuronGroup();
        this.component = component;
    }

    @Override
    public JMenu getProducerMenu() {
        if (component != null) {
            PotentialProducer producer = component.getAttributeManager()
                    .createPotentialProducer(neuronGroup, "getActivations",
                            double[].class);
            producer.setCustomDescription("Neuron Group: "
                    + neuronGroup.getLabel());
            JMenu producerMenu = new CouplingMenuProducer(
                    "Send Vector Coupling to", component.getWorkspace(),
                    producer);
            return producerMenu;
        }
        return null;
    }

    @Override
    public JMenu getConsumerMenu() {
        if (component != null) {
            PotentialConsumer consumer = component.getAttributeManager()
                    .createPotentialConsumer(neuronGroup, "setInputValues",
                            double[].class);
            consumer.setCustomDescription("Neuron Group: "
                    + neuronGroup.getLabel());
            JMenu menu = new CouplingMenuConsumer(
                    "Receive Vector Coupling from", component.getWorkspace(),
                    consumer);
            return menu;
        }
        return null;
    }

}
