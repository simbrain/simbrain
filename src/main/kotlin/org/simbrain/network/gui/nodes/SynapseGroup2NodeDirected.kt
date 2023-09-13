package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.simbrain.util.component1
import org.simbrain.util.component2
import org.simbrain.util.line
import org.simbrain.util.p
import org.simbrain.util.widgets.BezierArrow
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
class SynapseGroup2NodeDirected(private val synapseGroupNode: SynapseGroup2Node) : PNode(), SynapseGroup2Node.Arrow {

    private val source = synapseGroupNode.synapseGroup.source
    private val target = synapseGroupNode.synapseGroup.target
    private fun isBidirectional() = target.outgoingSg.any { it.target == source }

    private val arrow: BezierArrow = bezierArrow {

        lateralOffset {
            if (isBidirectional()) 0.35 else 0.5
        }

        onUpdated { curve ->
            val offset = if (isBidirectional()) 0.75 else 0.5
            val (x, y) = curve?.p(offset) ?: line(source.location, target.location).p(offset)
            SwingUtilities.invokeLater { synapseGroupNode.interactionBox.centerFullBoundsOnPoint(x, y) }
        }

    }.also { addChild(it) }

    override fun layoutChildren() = arrow.layout(source.outlines, target.outlines, isBidirectional())

}