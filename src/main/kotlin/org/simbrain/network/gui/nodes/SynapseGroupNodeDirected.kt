/**
 * The single arrow a synapse group is drawn as when its synapses are not showing: a curve between its two
 * collections, or a loop for a group from a collection to itself. It is laid out by the panel's connection layout
 * through the group's [SynapseGroupNode], which also places the label box on it.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.simbrain.network.gui.JoinedLayers
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RECIPROCAL_ARROW_SHIFT
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.geom.CubicCurve2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

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

            // Beside the arrow of the group that runs the other way, when there is one
            lateralShift {
                if (isBidirectional()) RECIPROCAL_ARROW_SHIFT else 0.0
            }

            padding {
                head = arrowSize
                tail = 0.0
                tailBesideAnother = arrowSize
            }

        }
    }.also { addChild(it) }

    /** The shaft of the arrow as last laid out, or null for a loop, or for collections no arrow can be drawn between. */
    var shaft: CubicCurve2D? = null
        private set

    /**
     * How far along the [shaft] the group's label box goes when nothing is in the way. Beside the arrow of a group
     * that runs the other way it goes toward the target, clear of that group's label box.
     */
    val labelPosition get() = if (isBidirectional() && (arrow as? BezierArrow)?.hasOwnLane != true) 0.75 else 0.5

    /** Where the group's label box goes when there is no [shaft] to place it on, as last laid out. */
    var labelPoint: Point2D? = null
        private set

    /** The two collections the group joins. */
    internal val joined = JoinedLayers({ synapseGroupNode.sourceNode }, { synapseGroupNode.targetNode })

    /** Whether a group ran the other way between the two collections when the arrow was last drawn. */
    private var drawnBesideAnother = false

    /** Draw the arrow between the two collections as they now are, around any of [layers] that is in its way. */
    fun layout(layers: List<Rectangle2D>) {
        joined.arrowDrawn()
        drawnBesideAnother = isBidirectional()
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNodeBounds.centerLeft + point(15, 0)) { labelPoint = it }
            is BezierArrow -> {
                shaft = arrow.layout(
                    sourceNodeBounds.outlines, targetNodeBounds.outlines, isBidirectional(), joined.blockers(layers)
                )
                labelPoint = line(source.location, target.location).p(labelPosition)
            }
        }
    }

    /**
     * Whether the arrow has to be drawn again: one of its two collections is not where or as large as it was when
     * the arrow was drawn, a group that runs the other way has been added or removed, or the layers in its way are
     * not the ones it was drawn around.
     */
    fun affectedBy(layers: List<Rectangle2D>) =
        joined.changedSinceDrawn || isBidirectional() != drawnBesideAnother ||
            (arrow is BezierArrow && arrow.affectedBy(joined.blockers(layers)))

    /** Re-read the themed [NetworkPreferences.synapseGroupArrowColor] (e.g. after a light/dark switch). */
    fun updateColorFromPreferences() {
        val color = NetworkPreferences.synapseGroupArrowColor
        when (arrow) {
            is RecurrentArrow -> arrow.updateColor(color)
            is BezierArrow -> arrow.updateColor(color)
        }
    }

}