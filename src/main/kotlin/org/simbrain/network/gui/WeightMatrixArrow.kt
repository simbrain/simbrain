/**
 * The arrow of a weight matrix: a curve between two layers, or a loop for a matrix from a layer to itself. It is drawn
 * among the connections, as the matrix node's connection drawing, and laid out in two steps by the panel's
 * [ConnectionLayout]: the arrow first, then what the matrix draws on it.
 */
package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.NeuronCollectionNode
import org.simbrain.network.gui.nodes.ShaftItem
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RECIPROCAL_ARROW_SHIFT
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.geom.CubicCurve2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

class WeightMatrixArrow(private val weightMatrixNode: WeightMatrixNode) : PNode() {

    private val source get() = weightMatrixNode.model.source
    private val sourceNodeBounds get() = with(weightMatrixNode.sourceNode) {
        when (this) {
            is NeuronCollectionNode -> outlinedObjects.globalFullBounds
            else -> this.globalBounds
        }
    }
    private val sourceNodeOutlines get() = with(weightMatrixNode.sourceNode) {
        when (this) {
            is NeuronCollectionNode -> outlinedObjects.globalFullBounds.outlines
            else -> directionalOutlines(this.footprint, this.globalBounds)
        }
    }
    private val target get() = weightMatrixNode.model.target
    private val targetNodeBounds get() = with(weightMatrixNode.targetNode) {
        when (this) {
            is NeuronCollectionNode -> outlinedObjects.globalFullBounds
            else -> this.globalBounds
        }
    }
    private val targetNodeOutlines get() = with(weightMatrixNode.targetNode) {
        when (this) {
            is NeuronCollectionNode -> outlinedObjects.globalFullBounds.outlines
            else -> directionalOutlines(this.footprint, this.globalBounds)
        }
    }
    private fun isBidirectional() = target.outgoingConnectors.any { it.target == source }

    private val arrow = if (source == target) {
        RecurrentArrow(NetworkPreferences.connectorArrowColor)
    } else {
        bezierArrow {
            color = NetworkPreferences.connectorArrowColor

            padding {
                tail = when (source) {
                    is ArrayLayer -> 0.0
                    else -> defaultTail
                }
                head = when (target) {
                    is ArrayLayer -> 5.0 + arrowSize
                    else -> defaultHead
                }
            }

            // Beside the arrow of the matrix that runs the other way, when there is one
            lateralShift {
                if (isBidirectional()) RECIPROCAL_ARROW_SHIFT else 0.0
            }

        }
    }.also { addChild(it) }

    /** The shaft of the arrow as last laid out, or null for a loop, or for layers no arrow can be drawn between. */
    private var shaft: CubicCurve2D? = null

    /** Where what the matrix draws goes on a loop, as last laid out. */
    private var loopAnchor: Point2D? = null

    fun updateColorFromPreferences() {
        when (arrow) {
            is RecurrentArrow -> arrow.updateColorFromPreferences()
            is BezierArrow -> arrow.updateColorFromPreferences()
        }
    }

    /** The space taken up by the two layers the matrix joins. */
    private fun ownLayers(): List<Rectangle2D> =
        listOf(weightMatrixNode.sourceNode.footprint, weightMatrixNode.targetNode.footprint)

    /** The space the two layers took up when the arrow was last drawn. */
    private var drawnBetween: List<Rectangle2D>? = null

    /** The space taken up by the layers in [layers] that the matrix does not join. */
    private fun otherLayers(layers: List<Rectangle2D>): List<Rectangle2D> {
        val own = ownLayers()
        return layers.filter { layer -> own.none { it == layer } }
    }

    /** Draw the arrow between the two layers as they now are, around any of [layers] that is in its way. */
    fun layout(layers: List<Rectangle2D>) {
        drawnBetween = ownLayers()
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNodeBounds.centerLeft + point(15, 0)) { loopAnchor = it }
            is BezierArrow -> {
                shaft = arrow.layout(sourceNodeOutlines, targetNodeOutlines, isBidirectional(), otherLayers(layers))
            }
        }
    }

    /**
     * Whether the arrow has to be drawn again: one of its two layers is not where or as large as it was when the
     * arrow was drawn, one of [layers] is now in its way, or it goes around one that may have moved.
     */
    fun affectedBy(layers: List<Rectangle2D>) =
        ownLayers() != drawnBetween || (arrow is BezierArrow && arrow.affectedBy(otherLayers(layers)))

    /** What the matrix draws, to be placed on the arrow as [layout] last drew it, or null if it is not on a shaft. */
    fun matrixToPlace(): ShaftItem? {
        val curve = shaft ?: return null
        if (arrow !is BezierArrow) return null
        // Beside the arrow of a matrix that runs the other way, each matrix draws in the half of the gap nearer its
        // source. An arrow with a lane of its own has the whole of it
        val sharesGap = isBidirectional() && !arrow.hasOwnLane
        return ShaftItem(
            curve,
            weightMatrixNode.drawing(),
            ownLayers = listOf(weightMatrixNode.sourceNode.footprint, weightMatrixNode.targetNode.footprint),
            preferred = if (sharesGap) 0.25 else 0.5,
            shared = sharesGap
        )
    }

    /** Where what the matrix draws goes when it is not on a shaft: on the loop, or between layers too close for an arrow. */
    fun placeOffShaft(): Rectangle2D? {
        if (arrow is RecurrentArrow) return loopAnchor?.let { weightMatrixNode.placeAt(it.x, it.y) }
        val (x, y) = line(source.location, target.location).p(if (isBidirectional()) 0.25 else 0.5)
        return weightMatrixNode.placeAt(x, y)
    }

}
