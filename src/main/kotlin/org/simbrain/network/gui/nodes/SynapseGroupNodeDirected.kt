package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.geom.Point2D

/**
 * PNode representation of a directed "green arrow" (representing a group of synapses) from one
 * [NeuronCollectionNode] to another.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 * @author Leo Yulin Li
 */
class SynapseGroupNodeDirected(private val synapseGroupNode: SynapseGroupNode) : PNode() {

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

            padding {
                head = arrowSize
                tail = 0.0
            }

        }
    }.also { addChild(it) }

    /** Where the group's label box goes on the arrow, as last laid out. */
    var labelPoint: Point2D? = null
        private set

    /** Draw the arrow between the two collections as they now are. */
    fun layout() {
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNodeBounds.centerLeft + point(15, 0)) { labelPoint = it }
            is BezierArrow -> {
                val bidirectional = isBidirectional()
                val offset = if (bidirectional) 0.75 else 0.5
                val shaft = arrow.layout(sourceNodeBounds.outlines, targetNodeBounds.outlines, bidirectional)
                labelPoint = shaft?.p(offset) ?: line(source.location, target.location).p(offset)
            }
        }
    }

    /** Re-read the themed [NetworkPreferences.synapseGroupArrowColor] (e.g. after a light/dark switch). */
    fun updateColorFromPreferences() {
        val color = NetworkPreferences.synapseGroupArrowColor
        when (arrow) {
            is RecurrentArrow -> arrow.updateColor(color)
            is BezierArrow -> arrow.updateColor(color)
        }
    }

}