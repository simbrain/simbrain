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
package org.simbrain.network.gui.nodes;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.simbrain.network.gui.NetworkDialogsKt.createNeuronGroupDialog;
import static org.simbrain.network.gui.NetworkPanelMenusKt.createCouplingMenu;

/**
 * PNode representation of a group of neurons. Contains an interaction box and
 * outlined objects (neuron nodes) as children. Compare {@link SubnetworkNode}.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
@SuppressWarnings("serial")
public class NeuronGroupNode extends AbstractNeuronCollectionNode {

    private static final int DEFAULT_BUFFER = 10;

    private final int buffer = DEFAULT_BUFFER;

    /**
     * Reference to represented group node.
     */
    private final NeuronGroup neuronGroup;

    /**
     * List of custom menu items added by subclasses.
     */
    private final List<JMenuItem> customMenuItems = new ArrayList<JMenuItem>();

    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param group        the neuron group
     */
    public NeuronGroupNode(NetworkPanel networkPanel, NeuronGroup group) {
        super(networkPanel, group);
        this.neuronGroup = group;
        setInteractionBox(new NeuronGroupInteractionBox(networkPanel));
        getInteractionBox().setText(neuronGroup.getLabel());
    }

    /**
     * Get a reference to the underlying neuron group.
     *
     * @return reference to the neuron group.
     */
    public NeuronGroup getNeuronGroup() {
        return neuronGroup;
    }

    /**
     * Add a custom menu item to the list.
     *
     * @param item the custom item to add
     */
    public void addCustomMenuItem(final JMenuItem item) {
        customMenuItems.add(item);
    }

    /**
     * Call update synapse node positions on all constituent neuron nodes.
     * Ensures synapse nodes are updated properly when this is moved.
     */
    public void updateSynapseNodePositions() {
        if (getNetworkPanel().isRunning()) {
            return;
        }
        for (NeuronNode neuronNode : getNeuronNodes()) {
            neuronNode.updateSynapseNodePositions();
        }
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    /**
     * Helper class to create the neuron group property dialog (since it is
     * needed in two places.).
     *
     * @return the neuron group property dialog.
     */
    public StandardDialog getPropertyDialog() {
        return createNeuronGroupDialog(getNetworkPanel(), this.neuronGroup);
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    public JPopupMenu getDefaultContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        // Edit Submenu
        Action editGroup = new AbstractAction("Edit...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = getPropertyDialog();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        };
        menu.add(editGroup);
        menu.add(renameAction);
        menu.add(removeAction);

        // Selection submenu
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select Neurons") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                selectNeurons();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction("Select Incoming Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                // TODO: restore functionality
                // List<SynapseNode> incomingNodes = new ArrayList<SynapseNode>();
                // for (Synapse synapse : neuronGroup.getIncomingWeights()) {
                //     incomingNodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));
                //
                // }
                // getNetworkPanel().clearSelection();
                // getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction("Select Outgoing Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                // TODO: restore functionality
                // List<SynapseNode> outgoingNodes = new ArrayList<SynapseNode>();
                // for (Synapse synapse : neuronGroup.getOutgoingWeights()) {
                //     outgoingNodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));
                //
                // }
                // getNetworkPanel().clearSelection();
                // getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // Clamping actions
        menu.addSeparator();
        setClampActionsEnabled();
        menu.add(clampNeuronsAction);
        menu.add(unclampNeuronsAction);

        // Connect neuron groups
        menu.addSeparator();
        Action setSource = new AbstractAction("Set Group as Source") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().getSelectionManager().clear();
                getNetworkPanel().getSelectionManager().set(NeuronGroupNode.this.getInteractionBox());
                getNetworkPanel().getSelectionManager().convertSelectedNodesToSourceNodes();
            }
        };
        menu.add(setSource);
        Action clearSource = new AbstractAction("Clear Source Neuron Groups") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                // TODO: behavior does not match description: this clear all source selection
                getNetworkPanel().getSelectionManager().clearAllSource();
            }
        };
        menu.add(clearSource);
        Action makeConnection = getNetworkPanel().getNetworkActions().getAddSynapseGroupAction();
        menu.add(makeConnection);

        // Add any custom menus for this type
        if (customMenuItems.size() > 0) {
            menu.addSeparator();
            for (JMenuItem item : customMenuItems) {
                menu.add(item);
            }
        }

        // Test Inputs action
        //menu.addSeparator();
        //menu.add(testInputsAction);

        // Recording action
        menu.addSeparator();
        menu.add(new RecordingAction());

        // Coupling menu
        menu.addSeparator();
        JMenu couplingMenu = createCouplingMenu(getNetworkPanel().getNetworkComponent(), neuronGroup);
        if (couplingMenu != null) {
            menu.add(couplingMenu);
        }

        return menu;
    }

    /**
     * Custom interaction box for Subnetwork node. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    public class NeuronGroupInteractionBox extends InteractionBox {

        public NeuronGroupInteractionBox(NetworkPanel net) {
            super(net);
        }

        @Override
        public NeuronGroupNode getNode() {
            return NeuronGroupNode.this;
        }

        @Override
        public NeuronGroup getModel() {
            return NeuronGroupNode.this.getNeuronGroup();
        }

        @Override
        public JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

        @Override
        public String getToolTipText() {
            return "NeuronGroup: " + neuronGroup.getId()
                    + " Location: (" + Utils.round(neuronGroup.getLocation().getX(), 2) + ","
                    + Utils.round(neuronGroup.getLocation().getY(), 2) + ")";
        }
    }

    @Override
    public NeuronGroup getModel() {
        return neuronGroup;
    }

    /**
     * Action for removing this group.
     */
    protected Action removeAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/RedX_small.png"));
            putValue(NAME, "Remove Neuron Group");
            putValue(SHORT_DESCRIPTION, "Remove neuron group");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            getNetworkPanel().getNetwork().removeNeuronGroup(neuronGroup);
        }
    };

    /**
     * Sets whether the clamping actions are enabled based on whether the
     * neurons are all clamped or not.
     * <p>
     * If all neurons are clamped already, then "clamp neurons" is disabled.
     * <p>
     * If all neurons are unclamped already, then "unclamp neurons" is disabled.
     */
    private void setClampActionsEnabled() {
        clampNeuronsAction.setEnabled(!neuronGroup.isAllClamped());
        unclampNeuronsAction.setEnabled(!neuronGroup.isAllUnclamped());
    }

    /**
     * Action for clamping neurons.
     */
    protected Action clampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Clamp.png"));
            putValue(NAME, "Clamp Neurons");
            putValue(SHORT_DESCRIPTION, "Clamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronGroup.setClamped(true);
        }
    };

    /**
     * Action for unclamping neurons.
     */
    protected Action unclampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Clamp.png"));
            putValue(NAME, "Unclamp Neurons");
            putValue(SHORT_DESCRIPTION, "Unclamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronGroup.setClamped(false);
        }
    };

    private boolean samePoint(Point2D a, Point2D b) {
        return a.getX() == b.getX() && a.getY() == b.getY();
    }

}
