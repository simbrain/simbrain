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

class WeightMatrixArrow(private val weightMatrixNode: WeightMatrixNode) : PNode() {

    private val source get() = weightMatrixNode.model.source
    private val sourceNodeBounds get() = with(weightMatrixNode.sourceNode) {
        when (this) {
            is NeuronCollectionNode -> outlinedObjects.globalFullBounds
            else -> this.globalBounds
        }
    }
    private val target get() = weightMatrixNode.model.target
    private val targetNodeBounds get() = with(weightMatrixNode.targetNode) {
        when (this) {
            is NeuronCollectionNode -> outlinedObjects.globalFullBounds
            else -> this.globalBounds
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

            onUpdated { curve ->
                val offset = if (isBidirectional()) 0.25 else 0.5
                val (x, y) = curve?.p(offset) ?: line(source.location, target.location).p(offset)
                weightMatrixNode.imageBox.centerFullBoundsOnPoint(x, y)
                weightMatrixNode.interactionBox.centerFullBoundsOnPoint(x, y - weightMatrixNode.imageBox.height / 2.0 - weightMatrixNode.interactionBox.fullBounds.height / 2.0)
            }

        }
    }.also { addChild(it) }

    fun updateColorFromPreferences() {
        when (arrow) {
            is RecurrentArrow -> arrow.updateColorFromPreferences()
            is BezierArrow -> arrow.updateColorFromPreferences()
        }
        layoutChildren()
    }

    override fun layoutChildren() {
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNodeBounds.centerLeft + point(15, 0)) { (x, y) ->
                weightMatrixNode.imageBox.centerFullBoundsOnPoint(x, y)
                weightMatrixNode.interactionBox.centerFullBoundsOnPoint(x, y - weightMatrixNode.imageBox.height / 2.0 - weightMatrixNode.interactionBox.fullBounds.height / 2.0)
            }
            is BezierArrow -> arrow.layout(sourceNodeBounds.outlines, targetNodeBounds.outlines, isBidirectional())
        }
    }
}
