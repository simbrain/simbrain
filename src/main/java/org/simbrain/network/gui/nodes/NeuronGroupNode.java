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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.events.NetworkTextEvents;
import org.simbrain.network.events.NeuronCollectionEvents;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

    private final HashMap<Port, HashMap<SynapseGroupNodeSimple, Point2D>> dockingPorts = new HashMap<>();

    {
        dockingPorts.put(Port.NORTH, new HashMap<>());
        dockingPorts.put(Port.SOUTH, new HashMap<>());
        dockingPorts.put(Port.EAST, new HashMap<>());
        dockingPorts.put(Port.WEST, new HashMap<>());
    }

    public HashMap<Port, HashMap<SynapseGroupNodeSimple, Point2D>> getDockingPorts() {
        return dockingPorts;
    }

    public enum Port {
        NORTH, SOUTH, EAST, WEST,;

        public static Port opposite(Port p) {
            switch (p) {
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case EAST:
                    return WEST;
                case WEST:
                    return EAST;
                default:
                    throw new IllegalArgumentException("No such port");
            }
        }
    }

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
        // Must do this after it's added to properly locate it
        getInteractionBox().updateText();

        NeuronCollectionEvents events = neuronGroup.getEvents();
        // TODO: Aren't these repeats of AbstractNeuronCollectionNode?
        events.onDelete(n -> removeFromParent());
        events.onLabelChange((o,n) -> updateText());
        //events.onMoved((o,n) -> syncToModel());

    }

    /**
     * Select the neurons in this group.
     */
    public void selectNeurons() {
        List<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            nodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(neuron));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
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

    @Override
    public boolean showNodeHandle() {
        return false;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    protected boolean hasToolTipText() {
        return false;
    }

    @Override
    protected String getToolTipText() {
        return null;
    }

    @Override
    protected boolean hasContextMenu() {
        return false;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        return null;
    }

    @Override
    protected boolean hasPropertyDialog() {
        return false;
    }

    /**
     * Helper class to create the neuron group property dialog (since it is
     * needed in two places.).
     *
     * @return the neuron group property dialog.
     */
    public StandardDialog getPropertyDialog() {
        return getNetworkPanel().getNeuronGroupDialog(this);
    }

    @Override
    public void resetColors() {

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
                List<SynapseNode> incomingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : neuronGroup.getIncomingWeights()) {
                    incomingNodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction("Select Outgoing Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> outgoingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : neuronGroup.getOutgoingWeights()) {
                    outgoingNodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
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
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(Collections.singleton(NeuronGroupNode.this.getInteractionBox()));
                getNetworkPanel().setSourceElements();
            }
        };
        menu.add(setSource);
        Action clearSource = new AbstractAction("Clear Source Neuron Groups") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().clearSourceElements();
            }
        };
        menu.add(clearSource);
        Action makeConnection = getNetworkPanel().getActionManager().getAction("addSynapseGroup");
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
        JMenu couplingMenu = getNetworkPanel().getCouplingMenu(neuronGroup);
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
        protected JDialog getPropertyDialog() {
            return NeuronGroupNode.this.getPropertyDialog();
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
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

        @Override
        protected String getToolTipText() {
            return "NeuronGroup: " + neuronGroup.getId()
                + " Location: (" + Utils.round(neuronGroup.getPosition().getX(),2) + ","
                + Utils.round(neuronGroup.getPosition().getY(),2) + ")";
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
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
            getNetworkPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
            getNetworkPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), this);
            getNetworkPanel().getActionMap().put(this, this);

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

    public void removeSynapseDock(Port port, SynapseGroupNodeSimple synGN) {
        dockingPorts.get(port).remove(synGN);
    }

    private boolean samePoint(Point2D a, Point2D b) {
        return a.getX() == b.getX() && a.getY() == b.getY();
    }

    @Override
    public ScreenElement getSelectionTarget() {
        return getInteractionBox();
    }
}
