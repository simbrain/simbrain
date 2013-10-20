/*
` * Part of Simbrain--a java-based neural network kit
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
package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.NeuronGroupDialog;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;

/**
 * PNode representation of a group of neurons.
 *
 * @author Jeff Yoshimi
 */
public class NeuronGroupNode extends GroupNode {

    /** Reference to represented group node. */
    private final NeuronGroup group;

    /**
     * Stroke for neuron groups when they are in a subnet. Somewhat lighter than
     * general groups to distinguish these from subnetworks.
     */
    private static final BasicStroke LAYER_OUTLINE_STROKE = new BasicStroke(1f);

    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the neuron group
     */
    public NeuronGroupNode(NetworkPanel networkPanel, NeuronGroup group) {
        super(networkPanel, group);
        this.group = group;
        if (group.getParentGroup() instanceof Subnetwork) {
            if (!((Subnetwork) group.getParentGroup()).displayNeuronGroups()) {
                this.removeChild(this.getInteractionBox());
                this.setStroke(null);
                return;
            }
        }
        if (!group.isTopLevelGroup()) {
            setStroke(LAYER_OUTLINE_STROKE);
            setStrokePaint(Color.gray);
        }
        setInteractionBox(new NeuronGroupNodenteractionBox(networkPanel));
        setContextMenu(getDefaultContextMenu());

    }

    /**
     * Returns the NeuronGroup to this NeuronGroupNode.
     *
     * @return the neuron group
     */
    public NeuronGroup getNeuronGroup(){
        return (NeuronGroup) getGroup();
    }


    /**
     * Custom interaction box for Neuron Group node.
     */
    private class NeuronGroupNodenteractionBox extends InteractionBox {

        public NeuronGroupNodenteractionBox(NetworkPanel net) {
            super(net, NeuronGroupNode.this);
        }

        @Override
        protected JDialog getPropertyDialog() {
            return new NeuronGroupDialog(getNetworkPanel(), group);
        }

        @Override
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

    };


    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    @Override
    public JPopupMenu getDefaultContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        // Edit Submenu
        Action editGroup = new AbstractAction("Edit...") {
            public void actionPerformed(final ActionEvent event) {
                JDialog dialog = new NeuronGroupDialog(getNetworkPanel(), group);
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        menu.add(editGroup);
        menu.add(removeGroupAction);

        // Coupling menu
        if ((getProducerMenu() != null) && (getConsumerMenu() != null)) {
            menu.addSeparator();
            menu.add(getProducerMenu());
            menu.add(getConsumerMenu());
        }

        // Selection submenu
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select neurons") {
            public void actionPerformed(final ActionEvent event) {
                selectNeurons();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction(
                "Select incoming synapses") {
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> incomingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : group.getIncomingWeights()) {
                    incomingNodes.add((SynapseNode) getNetworkPanel()
                            .getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction(
                "Select outgoing synapses") {
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> outgoingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : group.getOutgoingWeights()) {
                    outgoingNodes.add((SynapseNode) getNetworkPanel()
                            .getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // Add the menu...
        return menu;
    }

    /**
     * Select the neurons in this group.
     */
    private void selectNeurons() {
        List<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : group.getNeuronList()) {
            nodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(
                    neuron));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

}
