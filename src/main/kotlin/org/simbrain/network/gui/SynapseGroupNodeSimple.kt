package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.util.line
import org.simbrain.util.midPoint
import org.simbrain.util.widgets.DirectedCubicArrow

/**
 * PNode representation of a "green arrow" (representing a group of synapses) from one
 * NeuronGroup to another.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 * @author Leo Yulin Li
 */
class SynapseGroupNodeSimple(private val synapseGroupNode: SynapseGroupNode) : PNode(), SynapseGroupNode.Arrow {

    private val arrow = DirectedCubicArrow().also { addChild(it) }

    override fun layoutChildren() {

        // check bidirectional
        if (synapseGroupNode.synapseGroup.targetNeuronGroup.outgoingSg
                        .any { it.targetNeuronGroup == synapseGroupNode.synapseGroup.sourceNeuronGroup }) {
            arrow.t = 0.35
        } else {
            arrow.t = 0.5
        }

        val midPoint = arrow.update(
                synapseGroupNode.synapseGroup.sourceNeuronGroup.outlines,
                synapseGroupNode.synapseGroup.targetNeuronGroup.outlines
        )

        if (midPoint == null) {
            with(synapseGroupNode.synapseGroup) {
                line(sourceNeuronGroup.location, targetNeuronGroup.location).midPoint
            }.let { synapseGroupNode.interactionBox.centerFullBoundsOnPoint(it.x, it.y) }
        } else {
            synapseGroupNode.interactionBox.centerFullBoundsOnPoint(midPoint.x, midPoint.y)
        }
    }
}