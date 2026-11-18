/**
 * The two layers a connector's arrow joins, as the layout of the arrow needs them: the space they take up, which of
 * the other layers on the canvas the arrow has to go around, and whether either has moved or changed size since the
 * arrow was drawn. Every kind of connector lays its arrow out against one of these.
 */
package org.simbrain.network.gui

import org.simbrain.network.gui.nodes.ScreenElement
import java.awt.geom.Rectangle2D

/**
 * @param source the node of the layer the arrow leaves, or null while it has none
 * @param target the node of the layer the arrow arrives at, or null while it has none
 */
internal class JoinedLayers(private val source: () -> ScreenElement?, private val target: () -> ScreenElement?) {

    /** The space taken up by the two layers. */
    fun footprints(): List<Rectangle2D> = listOfNotNull(source()?.footprint, target()?.footprint)

    /** The space the two layers took up when the arrow was last drawn. */
    private var drawnBetween: List<Rectangle2D>? = null

    /** Note that the arrow has just been drawn between the layers as they now are. */
    fun arrowDrawn() {
        drawnBetween = footprints()
    }

    /** Whether either layer is not where or as large as it was when the arrow was last drawn. */
    val changedSinceDrawn get() = footprints() != drawnBetween

    /**
     * Those of [layers] that the arrow goes around when they are in its way: every layer but the two it joins and
     * any that overlaps one of the two. An arrow starts or ends inside a layer that overlaps one of its own, as it
     * does between two collections inside a larger one, and no way around would take it clear.
     */
    fun blockers(layers: List<Rectangle2D>): List<Rectangle2D> {
        val own = footprints()
        return layers.filter { layer -> own.none { it == layer || it.intersects(layer) } }
    }
}
