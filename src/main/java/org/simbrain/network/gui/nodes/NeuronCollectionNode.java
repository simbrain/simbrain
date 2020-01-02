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
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SimpleFrame;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.math.NumericMatrix;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * PNode representation of a {@link NeuronCollection}.
 *
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class NeuronCollectionNode extends AbstractNeuronCollectionNode {

    /**
     * Reference to represented neuron collection
     */
    private final NeuronCollection neuronCollection;

    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param nc           the neuron collection
     */
    public NeuronCollectionNode(NetworkPanel networkPanel, NeuronCollection nc) {

        super(networkPanel, nc);

        this.neuronCollection = nc;

        NeuronCollectionInteractionBox interactionBox = new NeuronCollectionInteractionBox(networkPanel);
        interactionBox.setText(nc.getLabel());
        setInteractionBox(interactionBox);

        // TODO: Superclass handles these?
        //nc.addPropertyChangeListener(evt -> {
        //    if ("delete".equals(evt.getPropertyName())) {
        //        NeuronCollectionNode.this.removeFromParent();
        //        getOutlinedObjects().update(getNeuronNodes());
        //    } else if ("label".equals(evt.getPropertyName())) {
        //        interactionBox.setText(nc.getLabel());
        //        NeuronCollectionNode.this.updateText();
        //    } else if ("moved".equals(evt.getPropertyName())) {
        //        NeuronCollectionNode.this.syncToModel();
        //        getOutlinedObjects().update(getNeuronNodes());
        //    }
        //});

    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void syncToModel() {
        for (NeuronNode neuronNode : getNeuronNodes()) {
            neuronNode.pullViewPositionFromModel();
        }
    }

    @Override
    public void offset(double dx, double dy) {
        super.offset(dx, dy);
    }

    @Override
    public AbstractNeuronCollection getModel() {
        return neuronCollection;
    }

    /**
     * Helper class to create the neuron group property dialog (since it is needed in two places.).
     *
     * @return the neuron group property dialog.
     */
    public StandardDialog getPropertyDialog() {
        return null;
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    public JPopupMenu getNCContexMenu() {

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

        // Test Input Panel
        menu.addSeparator();
        Action testInputs = new AbstractAction("Input Data") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                // Input panel
                NumericMatrix matrix = new NumericMatrix() {

                    @Override
                    public void setData(double[][] data) {
                        neuronCollection.getInputManager().setData(data);
                    }

                    @Override
                    public double[][] getData() {
                        return neuronCollection.getInputManager().getData();
                    }
                };
                JPanel inputDataPanel = TestInputPanel.createTestInputPanel(getNetworkPanel(), neuronCollection.getNeuronList(), matrix);
                SimpleFrame.displayPanel(inputDataPanel);
            }
        };
        menu.add(testInputs);

        // Clamping actions
        menu.addSeparator();
        setClampActionsEnabled();
        menu.add(clampNeuronsAction);
        menu.add(unclampNeuronsAction);

        // Recording action
        menu.addSeparator();
        menu.add(new RecordingAction());

        // Coupling menu
        menu.addSeparator();
        JMenu couplingMenu = getNetworkPanel().getCouplingMenu(neuronCollection);
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
        private final Color BOX_COLOR = new Color(209, 255, 204);

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
        public NeuronCollectionNode getNode() {
            return NeuronCollectionNode.this;
        }

        @Override
        public NeuronCollection getModel() {
            return NeuronCollectionNode.this.getNeuronCollection();
        }

        @Override
        protected boolean hasPropertyDialog() {
            return false;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getNCContexMenu();
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
     * Action for removing this group.
     */
    protected Action removeAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/RedX_small.png"));
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
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Clamp.png"));
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
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Clamp.png"));
            putValue(NAME, "Unclamp Neurons");
            putValue(SHORT_DESCRIPTION, "Unclamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronCollection.setClamped(false);
        }
    };


}
