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
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
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

            lateralOffset {
                if (isBidirectional()) 0.35 else 0.5
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

    /** Draw the arrow between the two layers as they now are. */
    fun layout() {
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNodeBounds.centerLeft + point(15, 0)) { loopAnchor = it }
            is BezierArrow -> shaft = arrow.layout(sourceNodeOutlines, targetNodeOutlines, isBidirectional())
        }
    }

    /**
     * Place what the matrix draws on the arrow as [layout] last drew it, clear of [obstacles] where there is room.
     *
     * @return the space it now takes up
     */
    fun placeMatrix(obstacles: Obstacles): Rectangle2D? {
        if (arrow is RecurrentArrow) {
            return loopAnchor?.let { weightMatrixNode.placeAt(it.x, it.y) }
        }
        val bidirectional = isBidirectional()
        val offset = if (bidirectional) 0.25 else 0.5
        val curve = shaft
        return if (curve != null) {
            val ownLayers = listOf(weightMatrixNode.sourceNode.footprint, weightMatrixNode.targetNode.footprint)
            weightMatrixNode.placeOnShaft(curve, offset, shared = bidirectional, obstacles, ownLayers)
        } else {
            val (x, y) = line(source.location, target.location).p(offset)
            weightMatrixNode.placeAt(x, y)
        }
    }
}
