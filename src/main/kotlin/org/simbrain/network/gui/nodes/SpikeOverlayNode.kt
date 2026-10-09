/**
 * Draws spike highlights over the network's connections. Synapses sit in a cached tier that is redrawn only when they
 * change, and spikes flip every few iterations; if a synapse recolored its own line for each spike, the cache would be
 * thrown away nearly every frame in a spiking network. Instead the overlay, a tier of its own between the connections
 * and the neurons, draws each spiking synapse's line in the spiking color and its circle back on top, and in
 * spiking-only mode shows the synapses the edge tier keeps hidden.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.gui.dialogs.NetworkPreferences
import java.awt.Graphics2D

class SpikeOverlayNode : PNode() {

    private val spiking = LinkedHashSet<SynapseNode>()

    init {
        pickable = false
        childrenPickable = false
    }

    /** Starts or stops highlighting [node]; called on the EDT when its source neuron's drawn spike state flips. */
    fun setSpiking(node: SynapseNode, isSpiking: Boolean) {
        val changed = if (isSpiking) spiking.add(node) else spiking.remove(node)
        if (changed) refresh(node)
    }

    /** Stops highlighting [node] for good; called when it leaves the canvas. */
    fun remove(node: SynapseNode) {
        if (spiking.remove(node)) refresh(node)
    }

    /** Recomputes the overlay's bounds and repaints the area [node] covers, whether it was just added or removed. */
    private fun refresh(node: SynapseNode) {
        val area = PBounds(node.globalFullBounds)
        val union = PBounds()
        spiking.forEach { union.add(it.globalFullBounds) }
        setBounds(union)
        repaintFrom(PBounds(globalToLocal(area)), this)
    }

    override fun paint(paintContext: PPaintContext) {
        val g = paintContext.graphics
        for (node in spiking) {
            if (node.parent == null || !node.synapse.isVisible) continue
            drawPath(g, node.line ?: continue, fill = false)
            drawPath(g, node.circleNode ?: continue, fill = true)
        }
    }

    /** Draws one of a synapse's shapes in global coordinates: the line stroked in the spiking color, or the circle as drawn. */
    private fun drawPath(g: Graphics2D, path: PPath, fill: Boolean) {
        val saved = g.transform
        g.transform(path.getLocalToGlobalTransform(null))
        if (fill) {
            path.paint?.let {
                g.paint = it
                g.fill(path.pathReference)
            }
            path.strokePaint?.let {
                g.paint = it
                g.stroke = path.stroke
                g.draw(path.pathReference)
            }
        } else {
            g.paint = NetworkPreferences.spikingColor
            g.stroke = path.stroke
            g.draw(path.pathReference)
        }
        g.transform = saved
    }
}
