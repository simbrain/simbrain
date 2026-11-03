/**
 * Draws spike highlights over the network's connections. Synapses sit in a cached tier that is redrawn only when they
 * change, and spikes flip every few iterations; if a synapse recolored its own line for each spike, the cache would be
 * thrown away nearly every frame in a spiking network. Instead the overlay, a tier of its own between the connections
 * and the neurons, draws each spiking synapse's line in the spiking color and its circle and strength label back on
 * top, and in spiking-only mode shows the synapses the edge tier keeps hidden. Its bounds are the union of the spiking
 * synapses' bounds, recomputed when Piccolo next validates bounds after a highlight starts or stops or a highlighted
 * synapse moves, so a drag pays for one union per frame rather than one per synapse moved.
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

    /**
     * Keeps the bounds covering [node] after it moves, so Piccolo does not cull the highlight at its old place. The
     * move itself repaints the old and new areas.
     */
    fun moved(node: SynapseNode) {
        if (node in spiking) invalidateFullBounds()
    }

    /** Marks the overlay's bounds out of date and repaints the area [node] covers, whether it was just added or removed. */
    private fun refresh(node: SynapseNode) {
        val area = PBounds(node.globalFullBounds)
        invalidateFullBounds()
        repaintFrom(PBounds(globalToLocal(area)), this)
    }

    override fun computeFullBounds(dstBounds: PBounds?): PBounds {
        val result = dstBounds ?: PBounds()
        result.resetToZero()
        spiking.forEach { result.add(globalToLocal(PBounds(it.globalFullBounds))) }
        return result
    }

    override fun paint(paintContext: PPaintContext) {
        val g = paintContext.graphics
        for (node in spiking) {
            if (node.parent == null || !node.synapse.isVisible) continue
            drawSpikingLine(g, node.line ?: continue)
            drawAsDrawn(g, node.circle ?: continue)
            val saved = g.transform
            g.transform(node.getLocalToGlobalTransform(null))
            node.paintStrengthLabel(g)
            g.transform = saved
        }
    }

    /** Strokes a synapse's line in global coordinates in the spiking color. */
    private fun drawSpikingLine(g: Graphics2D, line: PPath) = inGlobalCoordinates(g, line) {
        g.paint = NetworkPreferences.spikingColor
        g.stroke = line.stroke
        g.draw(line.pathReference)
    }

    /** Draws a synapse's circle in global coordinates as the node draws it, back over the highlighted line. */
    private fun drawAsDrawn(g: Graphics2D, path: PPath) = inGlobalCoordinates(g, path) {
        path.paint?.let {
            g.paint = it
            g.fill(path.pathReference)
        }
        path.strokePaint?.let {
            g.paint = it
            g.stroke = path.stroke
            g.draw(path.pathReference)
        }
    }

    private inline fun inGlobalCoordinates(g: Graphics2D, path: PPath, draw: () -> Unit) {
        val saved = g.transform
        g.transform(path.getLocalToGlobalTransform(null))
        draw()
        g.transform = saved
    }
}
