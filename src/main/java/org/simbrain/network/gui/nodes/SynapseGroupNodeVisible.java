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
public class SynapseGroupNodeVisible extends SynapseGroupNode {

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group        the synapse group
     */
    public SynapseGroupNodeVisible(final NetworkPanel networkPanel, final SynapseGroup group) {
        super(networkPanel, group);
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
        interactionBox.centerFullBoundsOnPoint(x, y);
    }

}
