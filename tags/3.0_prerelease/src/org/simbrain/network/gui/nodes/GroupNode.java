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

import java.util.List;

/**
 * Interface for all PNodes representing Simbrain groups.
 */
public interface GroupNode {

    /**
     * Update all pnode constituents of this group node to update their
     * visible state (in the case of synapse group nodes, only do this
     * if there are visible synapses).
     */
    void updateConstituentNodes();

    /**
     * Returns a  list of child interaction boxes, or null if
     * it does not contain an interaction box.  Note that this is not a
     * recursive list of all interaction boxes below this one.   Rather,
     * some group nodes have multiple interaction boxes (currently just
     * SynapseGroupNodeBidirectional).
     *
     * @return the node's interaction box
     */
    List<InteractionBox> getInteractionBoxes();

}
