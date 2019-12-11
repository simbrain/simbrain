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
import org.simbrain.network.gui.NetworkPanel;

/**
 * PNode representation of a group of synapses, where the synapses themselves
 * are visible.
 *
 * @author jyoshimi
 */
public class SynapseGroupNodeVisible extends PNode implements SynapseGroupNode.Arrow {

    private SynapseGroupNode parent;

    /**
     * Create a Synapse Group PNode.
     */
    public SynapseGroupNodeVisible(final NetworkPanel networkPanel, final SynapseGroupNode parent) {
        this.parent = parent;

        parent.getSynapseGroup().getAllSynapses().forEach( s -> {
            NeuronNode sourceNode = (NeuronNode) networkPanel.getObjectNodeMap().get(s.getSource());
            NeuronNode targetNode = (NeuronNode) networkPanel.getObjectNodeMap().get(s.getTarget());
            if (sourceNode != null && targetNode != null) {
                SynapseNode synapseNode = new SynapseNode(networkPanel, sourceNode, targetNode, s);
                addChild(synapseNode);
            }
        });
        lowerToBottom();
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        double srcX = parent.getSynapseGroup().getSourceNeuronGroup().getCenterX();
        double srcY = parent.getSynapseGroup().getSourceNeuronGroup().getCenterY();
        double tarX = parent.getSynapseGroup().getTargetNeuronGroup().getCenterX();
        double tarY = parent.getSynapseGroup().getTargetNeuronGroup().getCenterY();
        double x = (srcX + tarX) / 2;
        double y = (srcY + tarY) / 2;
        parent.interactionBox.centerFullBoundsOnPoint(x, y);
    }

}
