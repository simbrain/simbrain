package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.util.line
import org.simbrain.util.p
import org.simbrain.util.widgets.BezierArrow

/**
 * PNode representation of a "green arrow" (representing a group of synapses) from one
 * NeuronGroup to another.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 * @author Leo Yulin Li
 */
class SynapseGroupNodeSimple(private val synapseGroupNode: SynapseGroupNode) : PNode(), SynapseGroupNode.Arrow {

    private val arrow = BezierArrow().also { addChild(it) }

    override fun layoutChildren() {

        // check bidirectional
        val isBidirectional = synapseGroupNode.synapseGroup.targetNeuronGroup.outgoingSg
                .any { it.targetNeuronGroup == synapseGroupNode.synapseGroup.sourceNeuronGroup }

        arrow.sourceEdgePercentage = if (isBidirectional) 0.35 else 0.5

        val curve = arrow.update(
                synapseGroupNode.synapseGroup.sourceNeuronGroup.outlines,
                synapseGroupNode.synapseGroup.targetNeuronGroup.outlines
        )

        val percentageOnCurve = if (isBidirectional) 0.75 else 0.5
        if (curve == null) {
            with(synapseGroupNode.synapseGroup) {
                line(sourceNeuronGroup.location, targetNeuronGroup.location).p(percentageOnCurve)
            }
        } else {
            curve.p(percentageOnCurve)
        }.let { synapseGroupNode.interactionBox.centerFullBoundsOnPoint(it.x, it.y) }
    }
}