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

import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.GroupNode;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * REDO: Rename to SubnetworkNode when subnetworks dismantled.
 * 
 * @author jyoshimi
 */
public class SubnetGroupNode extends GroupNode {

    /**
     * Create a subnetwork node
     * 
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public SubnetGroupNode(NetworkPanel networkPanel, Subnetwork group) {
        super(networkPanel, group);
    }

    @Override
    public void updateBounds() {

        PBounds bounds = new PBounds();
        for (PNode node : getOutlinedObjects()) {
            if (node.getVisible()) {
                bounds.add(node.getGlobalBounds());
            }
        }

        // Add a little extra height at top
        double inset = getOutlinePadding();
        bounds.setRect(bounds.getX() - inset, bounds.getY() - inset - 15,
                bounds.getWidth() + (2 * inset), bounds.getHeight()
                        + (2 * inset) + 15);

        setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                (float) bounds.getWidth(), (float) bounds.getHeight());

        updateInteractionBox();
    }

}
