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
package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.SynapsePolarityAndRandomizerPanel;
import org.simbrain.util.StandardDialog;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

/**
 * PNode representation of a group of synapses. Superclass of  more specific
 * types of synapsegroupnodes, where the contained synapses are either
 * individually visible or only visible through a single line representing the
 * whole group. For a sense of the design of this class (in to an interaction
 * box and outlined objects) see {@link SubnetworkNode}.
 *
 * @author Jeff Yoshimi
 */
public class SynapseGroupNode extends PNode implements GroupNode, PropertyChangeListener {

    /**
     * Parent network panel.
     */
    protected final NetworkPanel networkPanel;

    /**
     * Reference to represented group node.
     */
    protected final SynapseGroup synapseGroup;

    /**
     * The interaction box for this neuron group.
     */
    protected SynapseGroupInteractionBox interactionBox;

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group        the synapse group
     */
    protected SynapseGroupNode(NetworkPanel networkPanel, SynapseGroup group) {
        this.networkPanel = networkPanel;
        this.synapseGroup = group;
        // Note the children pnodes to outlined objects are created in
        // networkpanel and added externally to outlined objects
        interactionBox = new SynapseGroupInteractionBox(networkPanel, group, this);
        interactionBox.setText(synapseGroup.getLabel());
        addChild(interactionBox);
        // Must do this after it's added to properly locate it
        interactionBox.updateText();

        group.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("delete".equals(evt.getPropertyName())) {
                    SynapseGroupNode.this.removeFromParent();
                } else if ("label".equals(evt.getPropertyName())) {
                    SynapseGroupNode.this.updateText();
                } else if ("synapseVisibilityChanged".equals(evt.getPropertyName())) {
                    SynapseGroupNode.this.getNetworkPanel().
                        toggleSynapseVisibility((SynapseGroup) evt.getNewValue());
                } else if ("synapseAdded".equals(evt.getPropertyName())) {
                    SynapseGroupNode.this.getNetworkPanel().addSynapse(((Synapse) evt.getNewValue()));
                } else if ("synapseRemoved".equals(evt.getPropertyName())) {
                    SynapseGroupNode.this.getNetworkPanel().removeSynapse((Synapse) evt.getOldValue());
                }
            }
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // This is needed for synapse groups within subnetworks
        // to be updated properly when neuron groups are moved.
        layoutChildren();
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    public SynapseGroupInteractionBox getInteractionBox() {
        return interactionBox;
    }

    public SynapseGroup getSynapseGroup() {
        return synapseGroup;
    }

    /**
     * Update the text in the interaction box.
     */
    public void updateText() {
        interactionBox.setText(synapseGroup.getLabel());
        interactionBox.updateText();
    }

    @Override
    public void updateConstituentNodes() {
        // Do nothing since there are no constituent nodes. Synapses are
        // invisible.
    }

    @Override
    public List<InteractionBox> getInteractionBoxes() {
        return Collections.singletonList((InteractionBox) interactionBox);
    }

    /**
     * Show randomization dialog
     */
    public void showRandomizationDialog() {
        StandardDialog dialog = new StandardDialog();
        dialog.setContentPane(
            SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(dialog, this.getSynapseGroup()));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

}
