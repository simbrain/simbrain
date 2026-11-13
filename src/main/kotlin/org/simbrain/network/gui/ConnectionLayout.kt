/**
 * Lays out the connectors of a network panel in one pass: draws each one's arrow between its layers, then places
 * what each one draws on its arrow. A connector asks for this whenever something its layout depends on changes, such
 * as a layer moving, and however many ask before the pass runs, each is laid out once.
 *
 * The pass runs at the start of the canvas's own update, before it validates bounds and works out what to repaint.
 * A layer and the arrows attached to it are therefore redrawn in the same paint, and the layout is finished before
 * validation begins rather than run from inside it, where a node may only move what is below it.
 */
package org.simbrain.network.gui

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.PRoot
import javax.swing.SwingUtilities

/** A connector whose arrow, and what it draws on the arrow, a panel's [ConnectionLayout] lays out. */
interface LaidOutConnector {

    /** Draw the arrow between the connector's layers as they now are. */
    fun layoutArrow()

    /** Place what the connector draws on its arrow, which [layoutArrow] has just drawn. */
    fun placeOnArrow()
}

class ConnectionLayout(private val canvas: PCanvas) {

    /** Connectors waiting to be laid out, in the order they first asked. */
    private val pending = LinkedHashSet<LaidOutConnector>()

    private var updateScheduled = false

    private var running = false

    init {
        canvas.root.addInputSource(object : PRoot.InputSource {
            override fun processInput() = runPending()
        })
    }

    /** Lay [connector] out in the next pass. Safe from any thread. */
    fun request(connector: LaidOutConnector) {
        synchronized(pending) {
            pending.add(connector)
            if (updateScheduled) return
            updateScheduled = true
        }
        // The canvas may already be about to update, in which case the pass runs then and this finds nothing to do
        SwingUtilities.invokeLater {
            synchronized(pending) { updateScheduled = false }
            canvas.root.processInputs()
        }
    }

    /**
     * Run the pass now if any connector is waiting, for a caller on the event thread that is about to measure the
     * canvas outside its update. Does nothing during a pass.
     */
    fun flush() = runPending()

    private fun runPending() {
        if (running) return
        val connectors = synchronized(pending) { pending.toList().also { pending.clear() } }
        if (connectors.isEmpty()) return
        running = true
        try {
            // A connector that has left the canvas since it asked is skipped. One not yet on it asks again when added
            val onCanvas = connectors.filter { (it as? PNode)?.parent != null }
            onCanvas.forEachLogged { it.layoutArrow() }
            onCanvas.forEachLogged { it.placeOnArrow() }
        } finally {
            running = false
        }
    }

    /** One connector failing to lay out must not leave the rest of the batch where they were. */
    private inline fun List<LaidOutConnector>.forEachLogged(action: (LaidOutConnector) -> Unit) = forEach {
        try {
            action(it)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
