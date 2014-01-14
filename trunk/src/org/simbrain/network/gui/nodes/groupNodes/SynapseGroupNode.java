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
package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.dialogs.SynapseAdjustmentPanel;
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.OutlinedObjects;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.resource.ResourceManager;

import edu.umd.cs.piccolo.PNode;

/**
 * PNode representation of a group of synapses. Superclass of two more specific
 * types of synapsegroupnodes, where the contained synapses are either
 * individually visible or only visible through a single line representing the
 * whole group. For a sense of the design of this class (in to an interaction
 * box and outlined objects) see {@link SubnetworkNode}.
 *
 * @author Jeff Yoshimi
 */
public class SynapseGroupNode extends PNode implements PropertyChangeListener  {

    /** Parent network panel. */
    protected final NetworkPanel networkPanel;

    /** Reference to represented group node. */
    protected final SynapseGroup synapseGroup;

    /** The outlined objects (synapses) for this node. */
    protected final OutlinedObjects outlinedObjects;

    /** The interaction box for this neuron group. */
    protected SynapseGroupInteractionBox interactionBox;

    /**
     * Constant for use in group changed events, indicating that the visibility
     * of synpases in a synapse group has changed.
     */
    public static final String SYNAPSE_VISIBILITY_CHANGED = "synapseVisibilityChanged";

    /**
     * Menu for consumer actions. Set at the workspace level by
     * {@link NetworkPanelDesktop}.
     */
    private JMenu consumerMenu;

    /**
     * Menu for producer actions. Set at the workspace level by
     * {@link NetworkPanelDesktop}.
     */
    private JMenu producerMenu;

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    protected SynapseGroupNode(NetworkPanel networkPanel, SynapseGroup group) {
        this.networkPanel = networkPanel;
        this.synapseGroup = group;
        // Note the children pnodes to outlined objects are created in
        // networkpanel and added externally to outlined objects
        outlinedObjects = new OutlinedObjects();
        outlinedObjects.setDrawOutline(false);
        interactionBox = new SynapseGroupInteractionBox(networkPanel);
        interactionBox.setText(synapseGroup.getLabel());
        addChild(outlinedObjects);
        addChild(interactionBox);
        // Must do this after it's added to properly locate it
        interactionBox.updateText();
    }



    /**
     * Custom interaction box for Synapse Group nodes.
     */
    protected class SynapseGroupInteractionBox extends InteractionBox {

        /**
         * Construct the custom interaction box
         *
         * @param net parent network panel
         */
        public SynapseGroupInteractionBox(NetworkPanel net) {
            super(net);
        }

        @Override
        protected JDialog getPropertyDialog() {
            return new SynapseGroupDialog(getNetworkPanel(), synapseGroup);
        }

        @Override
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        public boolean isDraggable() {
            return false;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        layoutChildren();
    };

    /**
     * @return the networkPanel
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }


    /**
     * @return the interactionBox
     */
    public SynapseGroupInteractionBox getInteractionBox() {
        return interactionBox;
    }

    /**
     * @return the synapseGroup
     */
    public SynapseGroup getSynapseGroup() {
        return synapseGroup;
    }


    /**
     * @return the outlinedObjects
     */
    public OutlinedObjects getOutlinedObjects() {
        return outlinedObjects;
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    protected JPopupMenu getDefaultContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        // Edit
        Action editGroup = new AbstractAction("Edit...") {
            public void actionPerformed(final ActionEvent event) {
                JDialog dialog = new SynapseGroupDialog(getNetworkPanel(),
                        synapseGroup);
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        menu.add(editGroup);
        menu.add(removeGroup);

        // Weight adjustment stuff
        menu.addSeparator();
        Action adjustSynapses = new AbstractAction("Adjust Synapses...") {
            public void actionPerformed(final ActionEvent event) {
                selectSynapses();
                final SynapseAdjustmentPanel synapsePanel = new SynapseAdjustmentPanel(
                        getNetworkPanel(), synapseGroup.getSynapseList());
                JDialog dialog = new JDialog();
                dialog.setTitle("Adjust selected synapses");
                dialog.setContentPane(synapsePanel);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                dialog.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        synapsePanel.removeListeners();
                    }
                });
            }
        };
        menu.add(adjustSynapses);
        menu.add(new JMenuItem(showWeightMatrixAction));

        // Selection stuff
        menu.addSeparator();
        final JCheckBoxMenuItem tsvCheckBox =  new JCheckBoxMenuItem();
        Action toggleSynapseVisibility = new AbstractAction("Synapses Visible") {
            public void actionPerformed(final ActionEvent event) {
                if (synapseGroup.isDisplaySynapses()) {
                    synapseGroup.setDisplaySynapses(false);
                } else {
                    synapseGroup.setDisplaySynapses(true);
                }
                synapseGroup.getParentNetwork().fireGroupChanged(
                        new NetworkEvent<Group>(synapseGroup.getParentNetwork(),
                                synapseGroup, synapseGroup),
                        SynapseGroupNode.SYNAPSE_VISIBILITY_CHANGED);
                tsvCheckBox.setSelected(synapseGroup.isDisplaySynapses());
            }
        };
        tsvCheckBox.setAction(toggleSynapseVisibility);
        tsvCheckBox.setSelected(synapseGroup.isDisplaySynapses());
        menu.add(tsvCheckBox);

        // Selection stuff
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select Synapses") {
            public void actionPerformed(final ActionEvent event) {
                selectSynapses();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction(
                "Select Incoming Neurons") {
            public void actionPerformed(final ActionEvent event) {
                List<NeuronNode> incomingNodes = new ArrayList<NeuronNode>();
                for (Neuron neuron : synapseGroup.getSourceNeurons()) {
                    incomingNodes.add((NeuronNode) getNetworkPanel()
                            .getObjectNodeMap().get(neuron));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction(
                "Select Outgoing Neurons") {
            public void actionPerformed(final ActionEvent event) {
                List<NeuronNode> outgoingNodes = new ArrayList<NeuronNode>();
                for (Neuron neuron : synapseGroup.getTargetNeurons()) {
                    outgoingNodes.add((NeuronNode) getNetworkPanel()
                            .getObjectNodeMap().get(neuron));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // Coupling menu
        if ((getProducerMenu() != null) && (getConsumerMenu() != null)) {
            menu.addSeparator();
            menu.add(getProducerMenu());
            menu.add(getConsumerMenu());
        }

        return menu;
    }

    /**
     * Select the synapses in this group.
     */
    private void selectSynapses() {
        List<SynapseNode> nodes = new ArrayList<SynapseNode>();
        for (Synapse synapse : synapseGroup.getSynapseList()) {
            nodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(
                    synapse));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

    /**
     * Action for showing the weight matrix for this neuron group.
     */
    Action showWeightMatrixAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("grid.png"));
            putValue(NAME, "Show Weight Matrix");
            putValue(SHORT_DESCRIPTION, "Show Weight Matrix");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            List<Neuron> sourceNeurons = ((SynapseGroup) SynapseGroupNode.this
                    .synapseGroup).getSourceNeurons();
            List<Neuron> targetNeurons = ((SynapseGroup) SynapseGroupNode.this
                    .synapseGroup).getTargetNeurons();
            JPanel panel = WeightMatrixViewer
                    .getWeightMatrixPanel(new WeightMatrixViewer(sourceNeurons,
                            targetNeurons, SynapseGroupNode.this
                                    .getNetworkPanel()));
            getNetworkPanel().displayPanel(panel, "Edit weights");
        }
    };

    /** Action for editing the group name. */
    protected Action renameGroup = new AbstractAction("Rename group...") {
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:",
                    synapseGroup.getLabel());
            synapseGroup.setLabel(newName);
        }
    };

    /**
     * Action for removing this group
     */
    protected Action removeGroup = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("RedX_small.png"));
            putValue(NAME, "Remove group...");
            putValue(SHORT_DESCRIPTION, "Remove synapse group...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            getNetworkPanel().getNetwork().removeGroup(synapseGroup);
        }
    };

    /**
     * @return the consumerMenu
     */
    public JMenu getConsumerMenu() {
        return consumerMenu;
    }

    /**
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
     * @param producerMenu the producerMenu to set
     */
    public void setProducerMenu(JMenu producerMenu) {
        this.producerMenu = producerMenu;
    }

}

