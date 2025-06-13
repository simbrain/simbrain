package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow

class WeightMatrixArrow(private val weightMatrixNode: WeightMatrixNode) : PNode() {

    private val source get() = weightMatrixNode.model.source
    private val sourceNode get() = weightMatrixNode.sourceNode
    private val target get() = weightMatrixNode.model.target
    private val targetNode get() = weightMatrixNode.targetNode
    private fun isBidirectional() = target.outgoingConnectors.any { it.target == source }

    private val arrow = if (source == target) {
        RecurrentArrow(NetworkPreferences.weightMatrixArrowColor)
    } else {
        bezierArrow {
            color = NetworkPreferences.weightMatrixArrowColor

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
            }

        }
    }.also { addChild(it) }

    override fun layoutChildren() {
        when (arrow) {
            is RecurrentArrow -> arrow.layout(sourceNode.globalBounds.centerLeft + point(15, 0)) { (x, y) -> weightMatrixNode.imageBox.centerFullBoundsOnPoint(x, y) }
            is BezierArrow -> arrow.layout(sourceNode.globalBounds.outlines, targetNode.globalBounds.outlines, isBidirectional())
        }
    }
}
