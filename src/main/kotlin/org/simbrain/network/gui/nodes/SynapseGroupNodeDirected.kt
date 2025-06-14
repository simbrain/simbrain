package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow
import javax.swing.SwingUtilities

/**
 * PNode representation of a directed "green arrow" (representing a group of synapses) from one
 * [AbstractNeuronCollectionNode] to another.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 * @author Leo Yulin Li
 */
class SynapseGroupNodeDirected(private val synapseGroupNode: SynapseGroupNode) : PNode(), SynapseGroupNode.Arrow {

    private val source = synapseGroupNode.synapseGroup.source
    private val sourceNodeBounds get() = synapseGroupNode.sourceNode.outlinedObjects.globalFullBounds
    private val target get() = synapseGroupNode.model.target
    private val targetNodeBounds get() = synapseGroupNode.targetNode.outlinedObjects.globalFullBounds
    private fun isBidirectional() = target.outgoingSg.any { it.target == source }

    private val arrow = if (source == target) {
        RecurrentArrow(NetworkPreferences.synapseGroupArrowColor)
    } else {
        bezierArrow {

            color = NetworkPreferences.synapseGroupArrowColor

            lateralOffset {
                if (isBidirectional()) 0.35 else 0.5
            }

            onUpdated { curve ->
                val offset = if (isBidirectional()) 0.75 else 0.5
                val (x, y) = curve?.p(offset) ?: line(source.location, target.location).p(offset)
                SwingUtilities.invokeLater { synapseGroupNode.interactionBox.centerFullBoundsOnPoint(x, y) }
            }

        }
    }.also { addChild(it) }

    override fun layoutChildren() {
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNodeBounds.centerLeft + point(15, 0)) { (x, y) -> synapseGroupNode.interactionBox.centerFullBoundsOnPoint(x, y) }
            is BezierArrow -> arrow.layout(sourceNodeBounds.outlines, targetNodeBounds.outlines, isBidirectional())
        }
    }

}