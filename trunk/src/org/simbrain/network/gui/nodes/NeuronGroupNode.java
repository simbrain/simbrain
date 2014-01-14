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

import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.desktop.NetworkPanelDesktop;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.NeuronGroupPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;

import edu.umd.cs.piccolo.PNode;

/**
 * PNode representation of a group of neurons. Contains an interaction box and
 * outlined objects (neuron nodes) as children. Compare {@link SubnetworkNode}.
 *
 * @author Jeff Yoshimi
 */
public class NeuronGroupNode extends PNode {

    /** Parent network panel. */
    private final NetworkPanel networkPanel;

    /** Reference to represented group node. */
    private final NeuronGroup neuronGroup;

    /** The interaction box for this neuron group. */
    private NeuronGroupInteractionBox interactionBox;

    /** The outlined objects (neurons) for this neuron group. */
    private final OutlinedObjects outlinedObjects;

    /** List of custom menu items added by subclasses. */
    private List<JMenuItem> customMenuItems = new ArrayList<JMenuItem>();

    /**
     * Menu for consumer actions. Set at the workspace level by
     * {@link NetworkPanelDesktop}.
     */
    private JMenu consumerMenu;

    /**
     * Menu for producer actions.
     */
    private JMenu producerMenu;

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
        this.networkPanel = networkPanel;
        this.neuronGroup = group;
        outlinedObjects = new OutlinedObjects();
        interactionBox = new NeuronGroupInteractionBox(networkPanel);
        interactionBox.setText(neuronGroup.getLabel());
        addChild(outlinedObjects);
        addChild(interactionBox);
        // Must do this after it's added to properly locate it
        interactionBox.updateText();
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        interactionBox.setOffset(
                outlinedObjects.getFullBounds().getX(),
                outlinedObjects.getFullBounds().getY()
                        - interactionBox.getHeight() + 1);
    }

    /**
     * Select the neurons in this group.
     */
    private void selectNeurons() {
        List<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            nodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(
                    neuron));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

    /**
     * @return the networkPanel
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
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
     * @return the outlinedObjects
     */
    public OutlinedObjects getOutlinedObjects() {
        return outlinedObjects;
    }


    /**
     * Update positions of all neurons being represented.
     */
    public void pushViewPositionToModel() {
        for (Object object : outlinedObjects.getChildrenReference()) {
            if (object instanceof NeuronNode) {
                ((NeuronNode)object).pushViewPositionToModel();
            }
        }

    }

    /**
     * Helper class to create the neuron group property dialog (since it is
     * needed in two places.)
     *
     * @return the neuron group property dialog.
     */
    private StandardDialog getPropertyDialog() {
        StandardDialog dialog = new StandardDialog() {
            private final NeuronGroupPanel panel;
            {
                panel = new NeuronGroupPanel(getNetworkPanel(), neuronGroup, this);
                setContentPane(panel);
            }

            @Override
            protected void closeDialogOk() {
                super.closeDialogOk();
                panel.commitChanges();
            }
        };
        return dialog;
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
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = getPropertyDialog();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        };
        menu.add(editGroup);
        menu.add(renameGroup);
        menu.add(removeGroup);

        // Selection submenu
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select Neurons") {
            public void actionPerformed(final ActionEvent event) {
                selectNeurons();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction(
                "Select Incoming Synapses") {
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> incomingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : neuronGroup.getIncomingWeights()) {
                    incomingNodes.add((SynapseNode) getNetworkPanel()
                            .getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction(
                "Select Outgoing Synapses") {
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> outgoingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : neuronGroup.getOutgoingWeights()) {
                    outgoingNodes.add((SynapseNode) getNetworkPanel()
                            .getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // Connect neuron groups
        menu.addSeparator();
        Action setSource = new AbstractAction("Set Group as Source") {
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(
                        Collections.singleton(NeuronGroupNode.this
                                .getInteractionBox()));
                getNetworkPanel().setSourceElements();
            }
        };
        menu.add(setSource);
        Action clearSource = new AbstractAction("Clear Source Neuron Groups") {
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().clearSourceElements();
            }
        };
        menu.add(clearSource);
        Action makeConnection = getNetworkPanel().getActionManager()
                .getAddSynapseGroupAction();
        menu.add(makeConnection);

        // Add any custom menus for this type
        if (customMenuItems.size() > 0) {
            menu.addSeparator();
            for (JMenuItem item : customMenuItems) {
                menu.add(item);
            }
        }

        // Coupling menu
        if ((getProducerMenu() != null) && (getConsumerMenu() != null)) {
            menu.addSeparator();
            menu.add(getProducerMenu());
            menu.add(getConsumerMenu());
        }

        // Add the menu...
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
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

    }

    /**
     * @return the interactionBox
     */
    public NeuronGroupInteractionBox getInteractionBox() {
        return interactionBox;
    }

    /**
     * Set a custom interaction box.
     *
     * @param interactionBox the interactionBox to set.
     */
    protected void setInteractionBox(NeuronGroupInteractionBox newBox) {
        this.removeChild(interactionBox);
        this.interactionBox = newBox;
        this.addChild(interactionBox);
        updateText();
    }


    /**
     * Update the text in the interaction box.
     */
    public void updateText() {
        interactionBox.setText(neuronGroup.getLabel());
        interactionBox.updateText();
    };

    /**
     * @return the consumerMenu
     */
    public JMenu getConsumerMenu() {
        return consumerMenu;
    }

    /**
     * Set at the workspace level by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop}.
     *
     * @param consumerMenu the consumerMenu to set
     */
    public void setConsumerMenu(JMenu consumerMenu) {
        this.consumerMenu = consumerMenu;
    }

    /**
     * @return the producerMenu
     */
    public JMenu getProducerMenu() {
        return producerMenu;
    }

    /**
     * Set at the workspace level by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop}.
     *
     * @param producerMenu the producerMenu to set
     */
    public void setProducerMenu(JMenu producerMenu) {
        this.producerMenu = producerMenu;
    }

    /** Action for editing the group name. */
    protected Action renameGroup = new AbstractAction("Rename group...") {
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:",
                    neuronGroup.getLabel());
            neuronGroup.setLabel(newName);
        }
    };

    /**
     * Action for removing this group
     */
    protected Action removeGroup = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("RedX_small.png"));
            putValue(NAME, "Remove group...");
            putValue(SHORT_DESCRIPTION, "Remove neuron group...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            getNetworkPanel().getNetwork().removeGroup(neuronGroup);
        }
    };


}
