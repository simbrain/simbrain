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

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;

/**
 * PNode representation of a group of synapses, where the synapses themselves
 * are visible.
 *
 * @author jyoshimi
 */
public class SynapseGroupNodeFull extends SynapseGroupNode {

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    public SynapseGroupNodeFull(final NetworkPanel networkPanel,
            final SynapseGroup group) {
        super(networkPanel, group);
        this.setPaint(null);
    }


    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        double srcX = synapseGroup.getSourceNeuronGroup().getCenterX();
        double srcY = synapseGroup.getSourceNeuronGroup().getCenterY();
        double tarX = synapseGroup.getTargetNeuronGroup().getCenterX();
        double tarY = synapseGroup.getTargetNeuronGroup().getCenterY();
        double x = (srcX + tarX) / 2;
        double y = (srcY + tarY) / 2;
        interactionBox.setOffset(x - interactionBox.getWidth() / 2, y
                - interactionBox.getHeight() / 2);

        // Old layout code.  Some maybe useful so not removing it yet.
//          PBounds bounds = new PBounds();
//          if (getOutlinedObjects().size() > 0) {
//              for (PNode node : getOutlinedObjects()) {
//                  PBounds childBounds = node.getGlobalBounds();
//                  bounds.add(childBounds);
//                  if (node instanceof SynapseNode) {
//                      // Recurrent synapses screw things up when they have area 0
//                      Rectangle synapseBounds = ((SynapseNode) node).getLine()
//                              .getBounds();
//                      double area = synapseBounds.getHeight()
//                              * synapseBounds.getWidth();
//                      if (area > 0) {
//                          bounds.add(((SynapseNode) node).getLine().getBounds());
//                      }
//                  }
//              }
  //
//              double inset = getOutlinePadding();
//              bounds.setRect(bounds.getX() - inset, bounds.getY() - inset,
//                      bounds.getWidth() + (2 * inset), bounds.getHeight()
//                              + (2 * inset));
  //
//              // Can also use setPathToEllipse
//              setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
//                      (float) bounds.getWidth(), (float) bounds.getHeight());
  //
//          } else {
//              // TODO Need to get reference to parent nodes.
//              System.err.println("Bounds are null");
//              bounds = null;
//          }

    }

}
