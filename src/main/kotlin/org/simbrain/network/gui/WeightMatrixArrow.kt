package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.Color

class WeightMatrixArrow(private val weightMatrixNode: WeightMatrixNode) : PNode() {

    private val source get() = weightMatrixNode.model.source
    private val target get() = weightMatrixNode.model.target
    private fun isBidirectional() = target.outgoingConnectors.any { it.target == source }

    private val arrow = if (source == target) {
        RecurrentArrow()
    } else {
        bezierArrow {
            color = Color.ORANGE

            padding {
                tail = when (source) {
                    is ArrayLayer -> 5.0
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
            is RecurrentArrow -> arrow.layout(source.location) { (x, y) -> weightMatrixNode.imageBox.centerFullBoundsOnPoint(x, y) }
            is BezierArrow -> arrow.layout(source.bound.outlines, target.bound.outlines, isBidirectional())
        }
    }
}
