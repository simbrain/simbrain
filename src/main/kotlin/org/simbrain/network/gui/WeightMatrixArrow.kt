package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.dl4j.MultiLayerNet
import org.simbrain.network.dl4j.NeuronArray
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RecurrentArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.Color

class WeightMatrixArrow(private val weightMatrixNode: WeightMatrixNode) : PNode() {

    private val source get() = weightMatrixNode.model.source
    private val target get() = weightMatrixNode.model.target
    private fun isBidirectional() = target.outgoingWeightMatrices.any { it.target == source }

    private val arrow = if (source == target) {
        RecurrentArrow()
    } else {
        bezierArrow {
            color = Color.ORANGE

            padding {
                tail = when (source) {
                    is NeuronArray -> 10.0
                    is MultiLayerNet -> 10.0
                    else -> default
                }
                head = when (target) {
                    is NeuronArray -> 5.0
                    is MultiLayerNet -> 5.0
                    else -> default
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

        }}.also { addChild(it) }

    override fun layoutChildren() = when (arrow) {
        is RecurrentArrow -> arrow.update(source.location) { (x, y) -> weightMatrixNode.imageBox.centerFullBoundsOnPoint(x, y)}
        is BezierArrow -> arrow.update(source.bound.outlines, target.bound.outlines, isBidirectional())
        else -> Unit
    }
}
