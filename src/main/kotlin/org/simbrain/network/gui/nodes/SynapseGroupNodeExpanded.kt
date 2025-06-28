package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.simbrain.network.gui.NetworkPanel


/**
 * "Expanded" representation of a synapse group in the sense that individual synapses are visible.
 * The synapse group itself is only visible via the interaction box.
 *
 * PNodes representing individual synapses are managed via the isVisible field in [Synapse]. Changes to visibility
 * fire an event which is received by [SynapseNode].
 */
class SynapseGroupNodeExpanded(networkPanel: NetworkPanel, val parent: SynapseGroupNode):
    PNode(), SynapseGroupNode.Arrow  {

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