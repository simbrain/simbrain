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
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;

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
     * The outlined objects (synapses) for this node.
     */
    protected final OutlinedObjects outlinedObjects;

    /**
     * The interaction box for this neuron group.
     */
    protected SynapseGroupInteractionBox interactionBox;

    /**
     * Constant for use in group changed events, indicating that the visibility
     * of synpases in a synapse group has changed.
     */
    public static final String SYNAPSE_VISIBILITY_CHANGED = "synapseVisibilityChanged";

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
        outlinedObjects = new OutlinedObjects();
        outlinedObjects.setDrawOutline(false);
        interactionBox = new SynapseGroupInteractionBox(networkPanel, group);
        interactionBox.setText(synapseGroup.getLabel());
        addChild(outlinedObjects);
        addChild(interactionBox);
        // Must do this after it's added to properly locate it
        interactionBox.updateText();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // This is needed for synapse groups within subnetworks
        // to be updated properly when neuron groups are moved.
        layoutChildren();
    }

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

}
