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
package org.simbrain.network.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.listeners.GroupAdapter;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronAdapter;
import org.simbrain.network.listeners.SynapseAdapter;

/**
 * Displays the hierarchy of neurons, synapses and groups in a network in a Tree
 * View.
 *
 * TODO: This class is slated for removal.  It is being left here for now as a 
 * scrap heap for building other components.   All code for responding to 
 * network selection events has been commented out.
 *
 * @author Jeff Yoshimi
 */
public class NetworkHierarchyPanel extends JScrollPane {

    /** The network panel. */
    private final NetworkPanel networkPanel;

    /** The root node for the tree. */
    private DefaultMutableTreeNode root;

    /** The underlying model for the JTree */
    private DefaultTreeModel model;

    /** A custom JTree. */
    private NetworkJTree tree;

    /**
     * Table associating network objects (neurons, synapses, groups) with nodes
     * in the jtree model. Used to get references to the jtree nodes when
     * responding to changes in the network.
     */
    //private HashMap<Object, DefaultMutableTreeNode> objectNodeMap = new HashMap<Object, DefaultMutableTreeNode>();

    /**
     * Construct the panel.
     *
     * @param networkPanel
     */
    public NetworkHierarchyPanel(NetworkPanel networkPanel) {
        super();
        this.networkPanel = networkPanel;

        // Set up tree
        root = new DefaultMutableTreeNode("Root Network");
        tree = new NetworkJTree(root);
        tree.setExpandsSelectedPaths(true);
        model = (DefaultTreeModel) tree.getModel();

        // Set up the panel
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.white);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = .5;
        gbc.weighty = .5;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(tree, gbc);
        setViewportView(panel);

        // Add all listeners
        addListeners();

    }

    /**
     * Custom JTree
     *
     * Code for custom text display is here.
     *
     * TODO: Add custom renderer here to make the JTree look sexy.
     *
     */
    private class NetworkJTree extends JTree {

        /**
         * Construct custom jtree.
         *
         * @param root root node.
         */
        public NetworkJTree(DefaultMutableTreeNode root) {
            super(root);
        }

        @Override
        public String convertValueToText(Object object, boolean selected,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (object instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) object)
                        .getUserObject();
                if (userObject instanceof Neuron) {
                    return ((Neuron) userObject).getId();
                } else if (userObject instanceof Synapse) {
                    return ((Synapse) userObject).getId();
                } else if (userObject instanceof Group) {
                    return ((Group) userObject).getId();
                }
            }
            return super.convertValueToText(object, selected, expanded, leaf,
                    row, hasFocus);
        }
    }

//    /**
//     * Sync selected node of jtree to selected neurons of network panel
//     */
//    private void syncSelection() {
//        tree.clearSelection();
//        for (Object networkElement : networkPanel.getSelectedModelElements()) {
//            if (networkElement instanceof Neuron) {
//                DefaultMutableTreeNode treeNode = objectNodeMap
//                        .get((Neuron) networkElement);
//                if (treeNode != null) {
//                    tree.addSelectionPath(new TreePath(treeNode.getPath()));
//                }
//            } else if (networkElement instanceof Synapse) {
//                DefaultMutableTreeNode treeNode = objectNodeMap
//                        .get((Synapse) networkElement);
//                if (treeNode != null) {
//                    tree.addSelectionPath(new TreePath(treeNode.getPath()));
//                }
//            }
//
//        }
//    }

    /**
     * Reset the tree.
     *
     * TODO: This method is inefficient and may not scale up well. Nodes should
     * be inserted directly in as needed. See the commented out code where the
     * NeuronAdapter is added.
     */
    private void reset() {

        root.removeAllChildren();
        model.nodeStructureChanged(root);

        // Neurons
        DefaultMutableTreeNode neurons = new DefaultMutableTreeNode(
                "Loose Neurons");
        model.insertNodeInto(neurons, root, 0);
        for (Neuron neuron : networkPanel.getNetwork().getNeuronList()) {
            DefaultMutableTreeNode neuronTreeNode = new DefaultMutableTreeNode(
                    neuron);
            model.insertNodeInto(neuronTreeNode, neurons, 0);
            //objectNodeMap.put(neuron, neuronTreeNode);
        }

        // Synapses
        DefaultMutableTreeNode synapses = new DefaultMutableTreeNode(
                "Loose Synapses");
        for (Synapse synapse : networkPanel.getNetwork().getSynapseList()) {
            DefaultMutableTreeNode synapseTreeNode = new DefaultMutableTreeNode(
                    synapse);
            model.insertNodeInto(synapseTreeNode, synapses, 0);
            //objectNodeMap.put(synapse, synapseTreeNode);
        }
        root.add(synapses);

        // Groups
        DefaultMutableTreeNode groups = new DefaultMutableTreeNode("Groups");
        for (Group group : networkPanel.getNetwork().getGroupList()) {
            DefaultMutableTreeNode groupTreeNode = new DefaultMutableTreeNode(
                    group.getId());
            model.insertNodeInto(groupTreeNode, groups, 0);
        }
        root.add(groups);

    }

    /**
     * Add all the listeners.
     */
    private void addListeners() {

        // Selection events on the tree
        tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (tree.getSelectionPaths() == null) {
                    return;
                }
                for (int i = 0; i < tree.getSelectionPaths().length; i++) {
                    if (tree.getSelectionPaths()[i] == null) {
                        break;
                    }
                    for (int j = 0; j < tree.getSelectionPaths()[i]
                            .getPathCount(); j++) {
                        Object object = tree.getSelectionPaths()[i]
                                .getPathComponent(j);
                        if (object != null) {
                            if (object instanceof DefaultMutableTreeNode) {
                                Object userObject = ((DefaultMutableTreeNode) object)
                                        .getUserObject();
                                if (userObject instanceof Neuron) {
                                    NeuronNode node = (NeuronNode) networkPanel
                                            .getObjectNodeMap().get(userObject);
                                    networkPanel.addSelection(node);
                                }
                                if (userObject instanceof Synapse) {
                                    SynapseNode node = (SynapseNode) networkPanel
                                            .getObjectNodeMap().get(userObject);
                                    networkPanel.addSelection(node);
                                }
                            }
                        }

                    }
                }
            }

        });

        // Respond to mouse clicks
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForLocation(e.getX(),
                            e.getY());
                    if (selRow != -1) {
                        // NeuronNode node = (NeuronNode)
                        // selPath.getLastPathComponent();
                        // networkPanel.showSelectedNeuronProperties();
                    }
                }
            }
        });

        // Respond to changes in what was selected in the network panel here
//        networkPanel.addSelectionListener(new NetworkSelectionListener() {
//
//            /** @see NetworkSelectionListener */
//            public void selectionChanged(final NetworkSelectionEvent event) {
//                // reset();
//                syncSelection();
//            }
//        });

        //
        // Code below is for changing the tree in response to changes in the
        // network occurs below
        //

        networkPanel.getNetwork().addSynapseListener(new SynapseAdapter() {
            @Override
            public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
                reset();
            }

            @Override
            public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
                reset();
                //objectNodeMap.remove(networkEvent.getSource());
            }

        });

        networkPanel.getNetwork().addNeuronListener(new NeuronAdapter() {
            @Override
            public void neuronAdded(NetworkEvent<Neuron> networkEvent) {
                reset();
                // NeuronNode node = (NeuronNode)
                // networkPanel.getObjectNodeMap().get(networkEvent.getObject());
                // System.out.println(node);
                // DefaultMutableTreeNode neuronTreeNode = new
                // DefaultMutableTreeNode(node);
                // model.insertNodeInto(neuronTreeNode, neurons, 0);
            }

            @Override
            public void neuronRemoved(NetworkEvent<Neuron> networkEvent) {
                reset();
                //objectNodeMap.remove(networkEvent.getSource());
            }
        });

        networkPanel.getNetwork().addGroupListener(new GroupAdapter() {

            @Override
            public void groupAdded(NetworkEvent<Group> e) {
                reset();
            }

            @Override
            public void groupRemoved(NetworkEvent<Group> e) {
                reset();
            }

        });

    }

}
