/**
 * Decides when a connector's arrow should join other sides of its two layers than the ones it joins by itself.
 * Moving the arrow is the last thing tried for a drawing that is cramped: first the drawing slides along its arrow,
 * then it shrinks, and only when it would be left well under the size it has with the arrow to itself is the arrow
 * moved to where the drawing has more room. An arrow that has moved goes back once its usual way has clearly enough
 * room again, which is more room than it took to make it move, so that it does not flick between the two.
 */
package org.simbrain.network.gui

import org.simbrain.network.gui.nodes.ShaftItem
import org.simbrain.network.gui.nodes.ShaftPlacer
import org.simbrain.util.RectangleSides
import org.simbrain.util.widgets.ArrowRoute
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RoutedShaft
import java.awt.geom.Rectangle2D

internal object ArrowRouting {

    /** An arrow moves when its drawing would be under this fraction of the size it has with the arrow to itself. */
    private const val MOVE_BELOW = 0.5

    /** An arrow that has moved goes back when its usual way has room for this fraction of that size. */
    private const val RETURN_AT = 0.75

    /** An arrow only moves to where its drawing is at least this many times larger. */
    private const val WORTH_MOVING = 1.5

    /**
     * The sides [arrow] should join, or null for the ones it joins by itself.
     *
     * @param drawingOn what the connector draws, as it would be placed on a given shaft
     * @param placedSide the size the drawing was last given where the arrow now runs
     * @param layers the space taken up by each layer on the canvas
     * @param drawings the space taken up by what the other connectors draw
     */
    fun choose(
        arrow: BezierArrow,
        sourceOutlines: RectangleSides,
        targetOutlines: RectangleSides,
        bidirectional: Boolean,
        drawingOn: (RoutedShaft) -> ShaftItem,
        placedSide: Double,
        layers: List<Rectangle2D>,
        drawings: List<Rectangle2D>
    ): ArrowRoute? {
        /** The size the drawing would have on [route], placed on its own among the others where they now are. */
        fun sideOn(route: ArrowRoute?): Double? {
            val shaft = arrow.shaftFor(route, sourceOutlines, targetOutlines, bidirectional) ?: return null
            return ShaftPlacer.place(drawingOn(shaft), layers, drawings).side
        }

        val current = arrow.route
        val usualShaft = arrow.shaftFor(null, sourceOutlines, targetOutlines, bidirectional)
        if (usualShaft == null) {
            // No arrow can be drawn the usual way, as between two layers that all but touch: any other way is better
            if (current != null && arrow.shaftFor(current, sourceOutlines, targetOutlines, bidirectional) != null) return current
            return arrow.otherRoutes(sourceOutlines, targetOutlines, bidirectional)
                .mapNotNull { route -> sideOn(route)?.let { route to it } }
                .maxByOrNull { it.second }?.first
        }
        val full = ShaftPlacer.wantedSide(drawingOn(usualShaft))
        if (current != null) {
            // Placed on its own the drawing gets no more than it would placed with the others, so this errs toward
            // staying where it moved to
            val usual = sideOn(null) ?: return current
            if (usual >= RETURN_AT * full) return null
            // Stays for as long as the way it moved to can be taken
            return current.takeIf { arrow.shaftFor(it, sourceOutlines, targetOutlines, bidirectional) != null }
        }
        if (placedSide >= MOVE_BELOW * full) return null
        val best = arrow.otherRoutes(sourceOutlines, targetOutlines, bidirectional)
            .mapNotNull { route -> sideOn(route)?.let { route to it } }
            .maxByOrNull { it.second }
        return if (best != null && best.second >= WORTH_MOVING * placedSide) best.first else null
    }

}
