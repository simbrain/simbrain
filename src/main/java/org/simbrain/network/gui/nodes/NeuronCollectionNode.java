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

import org.piccolo2d.PNode;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * PNode representation of a {@link NeuronCollection}.
 *
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class NeuronCollectionNode extends PNode  {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Reference to represented neuron collection
     */
    private final NeuronCollection neuronCollection;

    /**
     * The interaction box for this neuron colection
     */
    private NeuronCollectionInteractionBox interactionBox;

    /**
     * The outlined objects (neurons) for this neuron group.
     */
    private final NeuronCollectionOutline outlinedObjects;

    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param nc           the neuron collection
     */
    public NeuronCollectionNode(NetworkPanel networkPanel, NeuronCollection nc) {

        this.networkPanel = networkPanel;
        this.neuronCollection = nc;

        outlinedObjects = new NeuronCollectionOutline();
        addChild(outlinedObjects);

        interactionBox = new NeuronCollectionInteractionBox(networkPanel);
        interactionBox.setText(nc.getLabel());
        addChild(interactionBox);

        //addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

        nc.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //System.out.println(evt.getPropertyName());
                if ("delete".equals(evt.getPropertyName())) {
                    NeuronCollectionNode.this.removeFromParent();
                } else if ("label".equals(evt.getPropertyName())) {
                    interactionBox.setText(nc.getLabel());
                    NeuronCollectionNode.this.updateText();
                } else if ("moved".equals(evt.getPropertyName())) {
                    // TODO: Is the below even used?
                    NeuronCollectionNode.this.syncToModel();
                }
            }
        });

    }

    /**
     * Override PNode layoutChildren method in order to properly set the positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (this.getVisible() && !networkPanel.isRunning()) {
            //TODO: Magic numbers below empirically set to fit.  Why? What diff from neuron group?
            interactionBox.setOffset(
                    outlinedObjects.getFullBounds().getX(),
                    outlinedObjects.getFullBounds().getY() - interactionBox.getFullBounds().getHeight() - 8);
        }
    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void syncToModel() {
        for (Object object : outlinedObjects.getNeuronNodeRefs()) {
            ((NeuronNode) object).pullViewPositionFromModel();
        }
    }

    @Override
    public void offset(double dx, double dy) {
        if (networkPanel.isRunning()) {
            return;
        }
        for (Object object : outlinedObjects.getNeuronNodeRefs()) {
            ((NeuronNode) object).offset(dx, dy);
        }
        neuronCollection.firePositionChanged();
    }

    /**
     * Add a neuron node to the group node.
     *
     * @param node to add
     */
    public void addNeuronNode(NeuronNode node) {
        outlinedObjects.addChildRef(node);
    }

    /**
     * Helper class to create the neuron group property dialog (since it is needed in two places.).
     *
     * @return the neuron group property dialog.
     */
    private StandardDialog getPropertyDialog() {
        return null;
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    public JPopupMenu getDefaultContextMenu() {

        JPopupMenu menu = new JPopupMenu();

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
                for (Synapse synapse : neuronCollection.getIncomingWeights()) {
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
                for (Synapse synapse : neuronCollection.getOutgoingWeights()) {
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

        // Coupling menu
        JMenu couplingMenu = networkPanel.getCouplingMenu(neuronCollection);
        if (couplingMenu != null) {
            menu.add(couplingMenu);
        }

        return menu;
    }

    /**
     * Select the neurons in this group.
     */
    public void selectNeurons() {
        List<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : neuronCollection.getNeuronList()) {
            nodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(neuron));
        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    public NeuronCollection getNeuronCollection() {
        return neuronCollection;
    }

    /**
     * Custom interaction box for Neuron Collections.
     */
    public class NeuronCollectionInteractionBox extends InteractionBox {

        /**
         * Color for the neuron collection interaction box
         */
        private final Color BOX_COLOR= new Color(209,255, 204);

        /**
         * Construct the interaction box
         */
        public NeuronCollectionInteractionBox(NetworkPanel net) {
            super(net);
            setPaint(BOX_COLOR);
            //setTransparency(.2f);
            updateText();
        }

        @Override
        protected JDialog getPropertyDialog() {
            return NeuronCollectionNode.this.getPropertyDialog();
        }

        @Override
        protected boolean hasPropertyDialog() {
            return false;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

        @Override
        protected String getToolTipText() {
            return "NeuronCollection: " + neuronCollection.getId()
                    + " Location: (" + Utils.round(neuronCollection.getPosition().getX(), 2) + ","
                    + Utils.round(neuronCollection.getPosition().getY(), 2) + ")";
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }
    }

    /**
     * @return the interactionBox
     */
    public NeuronCollectionInteractionBox getInteractionBox() {
        return interactionBox;
    }

    /**
     * Update the text in the interaction box.
     */
    public void updateText() {
        interactionBox.updateText();
    }


    /**
     * Action for editing the group name.
     */
    protected Action renameAction = new AbstractAction("Rename Neuron Collection...") {
        @Override
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:", neuronCollection.getLabel());
            neuronCollection.setLabel(newName);
        }
    };

    /**
     * Action for removing this group.
     */
    protected Action removeAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("RedX_small.png"));
            putValue(NAME, "Remove Neuron Collection.");
            putValue(SHORT_DESCRIPTION, "Remove neuron collection.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            getNetworkPanel().getNetwork().removeNeuronCollection(neuronCollection);
        }
    };

    /**
     * Sets whether the clamping actions are enabled based on whether the neurons are all clamped or not.
     * <p>
     * If all neurons are clamped already, then "clamp neurons" is disabled.
     * <p>
     * If all neurons are unclamped already, then "unclamp neurons" is disabled.
     */
    private void setClampActionsEnabled() {
        clampNeuronsAction.setEnabled(!neuronCollection.isAllClamped());
        unclampNeuronsAction.setEnabled(!neuronCollection.isAllUnclamped());
    }

    /**
     * Action for clamping neurons.
     */
    protected Action clampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Clamp Neurons");
            putValue(SHORT_DESCRIPTION, "Clamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronCollection.setClamped(true);
        }
    };

    /**
     * Action for unclamping neurons.
     */
    protected Action unclampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Unclamp Neurons");
            putValue(SHORT_DESCRIPTION, "Unclamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronCollection.setClamped(false);
        }
    };


}
