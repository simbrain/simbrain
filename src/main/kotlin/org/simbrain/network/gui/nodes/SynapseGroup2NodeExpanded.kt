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
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.simbrain.network.gui.NetworkPanel


/**
 * "Expanded" representation of a synapse group in the sense that individual synapses are visible.
 * The synapse group itself is only visible via the interaction box.
 */
class SynapseGroup2NodeExpanded(networkPanel: NetworkPanel, val parent: SynapseGroup2Node):
    PNode(), SynapseGroup2Node.Arrow  {

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    override fun layoutChildren() {
        val srcX: Double = parent.synapseGroup.source.centerX
        val srcY: Double = parent.synapseGroup.source.centerY
        val tarX: Double = parent.synapseGroup.target.centerX
        val tarY: Double = parent.synapseGroup.target.centerY
        val x = (srcX + tarX) / 2
        val y = (srcY + tarY) / 2
        parent.interactionBox.centerFullBoundsOnPoint(x, y)
    }

}