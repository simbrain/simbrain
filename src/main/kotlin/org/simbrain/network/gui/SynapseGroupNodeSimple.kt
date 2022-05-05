package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.util.component1
import org.simbrain.util.component2
import org.simbrain.util.line
import org.simbrain.util.p
import org.simbrain.util.widgets.bezierArrow

/**
 * PNode representation of a "green arrow" (representing a group of synapses) from one
 * NeuronGroup to another.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 * @author Leo Yulin Li
 */
class SynapseGroupNodeSimple(private val synapseGroupNode: SynapseGroupNode) : PNode(), SynapseGroupNode.Arrow {

    private val source = synapseGroupNode.synapseGroup.sourceNeuronGroup
    private val target = synapseGroupNode.synapseGroup.targetNeuronGroup
    // private fun isBidirectional() = target.outgoingSg.any { it.targetNeuronGroup == source }
    private fun isBidirectional() = false // Temp

    private val arrow = bezierArrow {

        lateralOffset {
            if (isBidirectional()) 0.35 else 0.5
        }

        onUpdated { curve ->
            val offset = if (isBidirectional()) 0.75 else 0.5
            val (x, y) = curve?.p(offset) ?: line(source.location, target.location).p(offset)
            synapseGroupNode.interactionBox.centerFullBoundsOnPoint(x, y)
        }

    }.also { addChild(it) }

    override fun layoutChildren() = arrow.layout(source.outlines, target.outlines, isBidirectional())

}