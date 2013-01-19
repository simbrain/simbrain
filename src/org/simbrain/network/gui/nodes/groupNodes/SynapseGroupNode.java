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

import java.awt.Rectangle;

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.SynapseNode;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * PNode representation of a group of synapses.
 *
 * @author jyoshimi
 */
public class SynapseGroupNode extends GroupNode {

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    public SynapseGroupNode(NetworkPanel networkPanel, SynapseGroup group) {
        super(networkPanel, group);
        //setStroke(null); // Comment this out to see outline
        // getInteractionBox().setPaint(Color.white);
        // setOutlinePadding(-30);
        setPickable(false);
    }

    @Override
    public void updateBounds() {
        PBounds bounds = new PBounds();
        if (getOutlinedObjects().size() > 0) {

            for (PNode node : getOutlinedObjects()) {
                PBounds childBounds = node.getGlobalBounds();
                bounds.add(childBounds);
                if (node instanceof SynapseNode) {
                    // Recurrent synapses screw things up when they have area 0
                    Rectangle synapseBounds = ((SynapseNode) node).getLine()
                            .getBounds();
                    double area = synapseBounds.getHeight()
                            * synapseBounds.getWidth();
                    if (area > 0) {
                        bounds.add(((SynapseNode) node).getLine().getBounds());
                    }
                }
            }

            double inset = getOutlinePadding();
            bounds.setRect(bounds.getX() - inset, bounds.getY() - inset,
                    bounds.getWidth() + (2 * inset), bounds.getHeight()
                            + (2 * inset));

            // Can also use setPathToEllipse
            setPathToEllipse((float) bounds.getX(), (float) bounds.getY(),
                    (float) bounds.getWidth(), (float) bounds.getHeight());

        } else {
            // TODO Need to get reference to parent nodes.
            System.err.println("Bounds are null");
            bounds = null;
        }

        updateInteractionBox();
    }

    @Override
    protected void updateInteractionBox() {
        InteractionBox interactionBox = getInteractionBox();
        interactionBox.setOffset(
                this.getBounds().getCenterX() - interactionBox.getWidth() * 2,
                this.getBounds().getCenterY() - interactionBox.getHeight());
        interactionBox.moveToFront();
    }

}
