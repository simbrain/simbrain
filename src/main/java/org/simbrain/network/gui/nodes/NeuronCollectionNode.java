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

import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static org.simbrain.network.gui.NetworkPanelMenusKt.createCouplingMenu;

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
    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void pullPositionFromModel() {
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

        menu.addSeparator();
        menu.add(getNetworkPanel().getNetworkActions().showApplyLayoutDialogAction(neuronCollection));

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
                getNetworkPanel().getSelectionManager().clear();
                neuronCollection.getIncomingWeights().forEach(Synapse::select);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction("Select Outgoing Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().getSelectionManager().clear();
                neuronCollection.getOutgoingWeights().forEach(Synapse::select);
            }
        };
        menu.add(selectOutgoingNodes);

        // Connect neuron connections
        menu.addSeparator();
        menu.add(getNetworkPanel().getNetworkActions().getConnectWithWeightMatrix());
        menu.add(getNetworkPanel().getNetworkActions().getConnectWithSynapseGroup());

        // Test Input Panel
        menu.addSeparator();
        Action testInputs = getNetworkPanel().getNetworkActions().createTestInputPanelAction(neuronCollection);
        menu.add(testInputs);
        Action addActivationToInput = getNetworkPanel().getNetworkActions().createAddActivationToInputAction(neuronCollection);
        menu.add(addActivationToInput);

        // Clamping actions
        menu.addSeparator();
        setClampActionsEnabled();
        menu.add(clampNeuronsAction);
        menu.add(unclampNeuronsAction);


        // Projection Plot Action
        menu.addSeparator();
        menu.add(SimbrainDesktop.INSTANCE.getActionManager().createCoupledPlotMenu(
                SimbrainDesktop.INSTANCE.getWorkspace().getCouplingManager().getProducer(neuronCollection, "getActivations"),
                neuronCollection.getLabel() + " Activations",
                "Plot Activations"
        ));

        // Coupling menu
        menu.addSeparator();
        JMenu couplingMenu = createCouplingMenu(getNetworkPanel().getNetworkComponent(), neuronCollection);
        if (couplingMenu != null) {
            menu.add(couplingMenu);
        }

        return menu;
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
        }

        @Override
        public JDialog getPropertyDialog() {
            return NeuronCollectionNode.this.getPropertyDialog();
        }

        @Override
        public NeuronCollection getModel() {
            return NeuronCollectionNode.this.getNeuronCollection();
        }

        @Override
        public JPopupMenu getContextMenu() {
            return getNCContexMenu();
        }

        @Override
        public String getToolTipText() {
            return "NeuronCollection: " + neuronCollection.getId()
                    + " Location: (" + Utils.round(neuronCollection.getLocation().getX(), 2) + ","
                    + Utils.round(neuronCollection.getLocation().getY(), 2) + ")";
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
            neuronCollection.delete();
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

    @Override
    public boolean acceptsSourceHandle() {
        return true;
    }

}
