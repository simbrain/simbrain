/**
 * Lays out the connectors of a network panel in one pass: draws the arrow of each connector whose layers changed or
 * whose way another layer now blocks, then places what every connector draws on its arrow. The drawings are placed
 * together, kept clear of the layers and of each other. Connectors and layers ask for a pass whenever something it
 * depends on changes, and however many ask before it runs, it runs once.
 *
 * Label boxes are drawn larger as the view zooms out, and a label box that sits on top of a drawing has to stay on
 * top of it, so a pass also runs when the size they are drawn at changes. Nothing else moves then, since the layout
 * is worked out for label boxes at full size, and panning the view changes nothing at all.
 *
 * The pass runs at the start of the canvas's own update, before it validates bounds and works out what to repaint.
 * A layer and the arrows attached to it are therefore redrawn in the same paint, and the layout is finished before
 * validation begins rather than run from inside it, where a node may only move what is below it.
 */
package org.simbrain.network.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.PRoot
import org.simbrain.network.gui.nodes.InteractionBox
import org.simbrain.network.gui.nodes.ShaftItem
import org.simbrain.network.gui.nodes.ShaftPlacement
import org.simbrain.network.gui.nodes.ShaftPlacer
import java.awt.geom.Rectangle2D
import javax.swing.SwingUtilities

/** A connector whose arrow, and what it draws on the arrow, a panel's [ConnectionLayout] lays out. */
interface LaidOutConnector {

    /**
     * Draw the arrow between the connector's layers as they now are. An arrow whose usual way passes through a layer
     * it does not join goes around that layer.
     *
     * @param layers the space taken up by each layer on the canvas, in canvas coordinates
     */
    fun layoutArrow(layers: List<Rectangle2D>)

    /**
     * Whether the arrow has to be drawn again although the connector did not ask: one of its two layers is not
     * where or as large as it was when the arrow was drawn, a layer it does not join is now in its way, or one that
     * was in its way has moved.
     */
    fun arrowAffectedBy(layers: List<Rectangle2D>): Boolean

    /**
     * Decide, now that the drawing has been placed, whether the arrow should join other sides of its two layers
     * than it does: other sides where the drawing was left cramped, or the usual ones again once they have room.
     * The arrow is then drawn that way by the next [layoutArrow].
     *
     * @param drawings the space taken up by what the other connectors draw, as just placed
     * @return whether the arrow should be drawn again
     */
    fun reconsiderRoute(layers: List<Rectangle2D>, drawings: List<Rectangle2D>): Boolean = false

    /**
     * What the connector draws on its arrow, as [layoutArrow] last drew it, for the panel to place among the
     * drawings of the other connectors, or null if it goes at a place of its own, as the image on a loop does.
     */
    fun drawingToPlace(): ShaftItem?

    /**
     * Put what the connector draws where [placement] says, or, without one, at the place of its own.
     *
     * @param placement where the drawing given by [drawingToPlace] goes, or null if there was none to place
     * @return the space the drawing now takes up, or null if the connector draws nothing
     */
    fun placeDrawing(placement: ShaftPlacement?): Rectangle2D?
}

/**
 * @param connectors the connectors on the canvas. Where a drawing has to keep clear of drawings that collide with
 * each other, those of the connectors earlier in this order keep their place
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

    /** The space each connector's drawing took up when last placed. */
    private val placed = HashMap<LaidOutConnector, Rectangle2D>()

    private var updateScheduled = false

    private var running = false

    /** How many passes have run. */
    internal var passes = 0
        private set

    /** How much larger than full size label boxes were drawn when the view last changed. */
    private var labelRescale = InteractionBox.zoomRescale(canvas.camera.viewScale)

    init {
        canvas.root.addInputSource(object : PRoot.InputSource {
            override fun processInput() = runPending()
        })
        canvas.camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM) {
            val rescale = InteractionBox.zoomRescale(canvas.camera.viewScale)
            if (rescale != labelRescale) {
                labelRescale = rescale
                requestPlacement()
            }
        }
    }

    /** Draw the arrow of [connector] again in the next pass, and place every drawing. Safe from any thread. */
    fun request(connector: LaidOutConnector) = schedule { pending.add(connector) }

    /**
     * Place every drawing again in the next pass, because something they keep clear of changed. Safe from any
     * thread. Does nothing during a pass: measuring a layer that has moved since it was last measured has the layer
     * report the change, which the pass doing the measuring already takes in.
     */
    fun requestPlacement() {
        if (running && SwingUtilities.isEventDispatchThread()) return
        schedule { placementPending = true }
    }

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
        passes++
        try {
            val all = connectors()
            val layerSpaces = layers()
            placed.keys.retainAll(all.toSet())
            fun drawingsBeside(connector: LaidOutConnector) = placed.filterKeys { it !== connector }.values.toList()
            // A connector that has left the canvas since it asked is skipped. One not yet on it asks again when added
            val asked = redraw.filter { (it as? PNode)?.parent != null }.toSet()
            var toDraw: Collection<LaidOutConnector> =
                asked + all.filter { it !in asked && logged { it.arrowAffectedBy(layerSpaces) } == true }
            // Where the drawings land can leave an arrow better joined to other sides, which moves the drawings
            // again. The rules for moving an arrow keep that from going on, and the count makes sure of it
            for (round in 1..ROUNDS) {
                toDraw.forEach { connector -> logged { connector.layoutArrow(layerSpaces) } }

                val items = all.map { connector -> logged { connector.drawingToPlace() } }
                // Drawings with a place of their own go first, since the others keep clear of them
                val fixed = mutableListOf<Rectangle2D>()
                all.forEachIndexed { index, connector ->
                    if (items[index] == null) {
                        val space = logged { connector.placeDrawing(null) }
                        if (space != null) {
                            fixed.add(space)
                            placed[connector] = space
                        } else {
                            placed.remove(connector)
                        }
                    }
                }
                val movable = all.indices.filter { items[it] != null }
                val placements = ShaftPlacer.placeAll(movable.map { items[it]!! }, layerSpaces, fixed)
                movable.forEachIndexed { n, index ->
                    val space = logged { all[index].placeDrawing(placements[n]) }
                    if (space != null) placed[all[index]] = space else placed.remove(all[index])
                }

                if (round == ROUNDS) break
                toDraw = all.filter { logged { it.reconsiderRoute(layerSpaces, drawingsBeside(it)) } == true }
                if (toDraw.isEmpty()) break
            }
        } finally {
            running = false
        }
    }

    private companion object {
        /** The most times arrows are drawn and drawings placed in one pass. */
        const val ROUNDS = 3
    }

    /** One connector failing to lay out must not leave the rest where they were. */
    private inline fun <T> logged(action: () -> T): T? = try {
        action()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}
