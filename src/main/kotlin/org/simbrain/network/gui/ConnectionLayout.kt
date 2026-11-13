/**
 * Lays out the connectors of a network panel in one pass: draws the arrow of each connector whose layers changed,
 * then places what every connector draws on its arrow, one after another, each kept clear of the layers and of the
 * drawings placed before it. Connectors and layers ask for a pass whenever something it depends on changes, and
 * however many ask before it runs, it runs once.
 *
 * The pass runs at the start of the canvas's own update, before it validates bounds and works out what to repaint.
 * A layer and the arrows attached to it are therefore redrawn in the same paint, and the layout is finished before
 * validation begins rather than run from inside it, where a node may only move what is below it.
 */
package org.simbrain.network.gui

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.PRoot
import java.awt.geom.Rectangle2D
import javax.swing.SwingUtilities

/** A connector whose arrow, and what it draws on the arrow, a panel's [ConnectionLayout] lays out. */
interface LaidOutConnector {

    /** Draw the arrow between the connector's layers as they now are. */
    fun layoutArrow()

    /**
     * Place what the connector draws on its arrow, as [layoutArrow] last drew it, clear of [obstacles] where there
     * is room.
     *
     * @return the space the drawing now takes up, which the connectors placed after this one keep clear of, or null
     * if it draws nothing
     */
    fun placeOnArrow(obstacles: Obstacles): Rectangle2D?
}

/**
 * What a connector's drawing is kept clear of, in canvas coordinates.
 *
 * @param layers the space taken up by each layer on the canvas. Layers are drawn over what connectors draw, so a
 * drawing that overlaps one is hidden by it
 * @param drawings the space taken up by the drawings of the connectors placed so far
 */
class Obstacles(val layers: List<Rectangle2D>, val drawings: List<Rectangle2D>)

/**
 * @param connectors the connectors on the canvas, in the order their drawings are placed. A connector earlier in
 * the order keeps its place, and one later moves out of its way
 * @param layers the space taken up by each layer on the canvas, in canvas coordinates
 */
class ConnectionLayout(
    private val canvas: PCanvas,
    private val connectors: () -> List<LaidOutConnector>,
    private val layers: () -> List<Rectangle2D>
) {

    /** Connectors whose arrows are waiting to be drawn again, in the order they first asked. */
    private val pending = LinkedHashSet<LaidOutConnector>()

    /** Whether drawings are waiting to be placed again although no arrow changed, as when a layer moved. */
    private var placementPending = false

    private var updateScheduled = false

    private var running = false

    init {
        canvas.root.addInputSource(object : PRoot.InputSource {
            override fun processInput() = runPending()
        })
    }

    /** Draw the arrow of [connector] again in the next pass, and place every drawing. Safe from any thread. */
    fun request(connector: LaidOutConnector) = schedule { pending.add(connector) }

    /** Place every drawing again in the next pass, because something they keep clear of changed. Safe from any thread. */
    fun requestPlacement() = schedule { placementPending = true }

    private inline fun schedule(note: () -> Unit) {
        synchronized(pending) {
            note()
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
        val redraw: List<LaidOutConnector>
        synchronized(pending) {
            if (pending.isEmpty() && !placementPending) return
            redraw = pending.toList()
            pending.clear()
            placementPending = false
        }
        running = true
        try {
            // A connector that has left the canvas since it asked is skipped. One not yet on it asks again when added
            redraw.filter { (it as? PNode)?.parent != null }.forEach { logged { it.layoutArrow() } }
            val obstacles = Obstacles(layers(), mutableListOf())
            connectors().forEach { connector ->
                logged {
                    connector.placeOnArrow(obstacles)?.let { (obstacles.drawings as MutableList).add(it) }
                }
            }
        } finally {
            running = false
        }
    }

    /** One connector failing to lay out must not leave the rest where they were. */
    private inline fun logged(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
