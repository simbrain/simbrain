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

import java.awt.BasicStroke;
import java.awt.Color;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.GroupNode;

/**
 * PNode representation of a group of neurons.
 * 
 * @author jyoshimi
 */
public class NeuronGroupNode extends GroupNode {
    
    /**
     * Stroke for neuron groups when they are in a subnet. Somewhat lighter than
     * general groups to distinguish these from subnetworks.
     */
    private static final BasicStroke LAYER_OUTLINE_STROKE = new BasicStroke(1f);
    
    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the neuron group
     */
    public NeuronGroupNode(NetworkPanel networkPanel, NeuronGroup group) {
        super(networkPanel, group);
        if (!group.isTopLevelGroup()) {
            setStroke(LAYER_OUTLINE_STROKE);
            setStrokePaint(Color.gray);            
        }
    }
    
}
