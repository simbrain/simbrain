/**
 * Decides when a connector's arrow should join other sides of its two layers than the ones it joins by itself.
 * Moving the arrow is the last thing tried for a drawing that is cramped: first the drawing slides along its arrow,
 * then it shrinks, and only when it has room for less than half the size it would have with the arrow to itself, or
 * no room even for its smallest size, is the arrow moved to where the drawing fits. An arrow that has moved goes
 * back once its usual way has clearly enough room again, which is more room than it took to make it move, so that
 * it does not flick between the two.
 */
package org.simbrain.network.gui

import org.simbrain.network.gui.nodes.ShaftItem
import org.simbrain.network.gui.nodes.ShaftPlacement
import org.simbrain.network.gui.nodes.ShaftPlacer
import org.simbrain.util.RectangleSides
import org.simbrain.util.widgets.ArrowRoute
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RoutedShaft
import java.awt.geom.Rectangle2D
import kotlin.math.max

internal object ArrowRouting {

    /** An arrow moves when its drawing has room for less than this fraction of the size it has with the arrow to itself. */
    private const val MOVE_BELOW = 0.5

    /** An arrow that has moved goes back when its usual way has this many times the room it took to make it move. */
    private const val RETURN_AT = 1.5

    /** An arrow only moves to where its drawing has at least this many times the room. */
    private const val WORTH_MOVING = 1.5

    /**
     * The sides [arrow] should join, or null for the ones it joins by itself.
     *
     * @param drawingOn what the connector draws, as it would be placed on a given shaft
     * @param placed where the drawing was last placed on the arrow as it now runs, or null if it was not
     * @param layers the space taken up by each layer on the canvas
     * @param drawings the space taken up by what the other connectors draw
     */
    fun choose(
        arrow: BezierArrow,
        sourceOutlines: RectangleSides,
        targetOutlines: RectangleSides,
        bidirectional: Boolean,
        drawingOn: (RoutedShaft) -> ShaftItem,
        placed: ShaftPlacement?,
        layers: List<Rectangle2D>,
        drawings: List<Rectangle2D>
    ): ArrowRoute? {
        /** Where the drawing would go on [route], placed on its own among the others where they now are. */
        fun placedOn(route: ArrowRoute?): ShaftPlacement? {
            val shaft = arrow.shaftFor(route, sourceOutlines, targetOutlines, bidirectional) ?: return null
            return ShaftPlacer.place(drawingOn(shaft), layers, drawings)
        }

        /** The other ways the arrow could run, with where the drawing would go on each. */
        fun others() = arrow.otherRoutes(sourceOutlines, targetOutlines, bidirectional)
            .mapNotNull { route -> placedOn(route)?.let { route to it } }
        val largestThenRoomiest = compareBy<Pair<ArrowRoute, ShaftPlacement>> { it.second.side }.thenBy { it.second.room }

        val current = arrow.route
        val usualShaft = arrow.shaftFor(null, sourceOutlines, targetOutlines, bidirectional)
        if (usualShaft == null) {
            // No arrow can be drawn the usual way, as between two layers that all but touch: any other way is better
            if (current != null && arrow.shaftFor(current, sourceOutlines, targetOutlines, bidirectional) != null) return current
            return others().maxWithOrNull(largestThenRoomiest)?.first
        }
        val usual = drawingOn(usualShaft)
        val smallest = usual.drawing.minSide
        // The room under which the drawing counts as cramped on the usual way
        val cramped = max(smallest, MOVE_BELOW * ShaftPlacer.wantedSide(usual))
        if (current != null) {
            // Placed on its own the drawing gets no more room than it would placed with the others, so this errs
            // toward staying where it moved to
            val onUsual = placedOn(null) ?: return current
            if (onUsual.room >= RETURN_AT * cramped) return null
            // Stays for as long as the way it moved to can be taken
            return current.takeIf { arrow.shaftFor(it, sourceOutlines, targetOutlines, bidirectional) != null }
        }
        if (placed == null || placed.room >= cramped) return null
        return others()
            .filter { (_, there) ->
                there.room >= smallest && there.room >= WORTH_MOVING * placed.room && there.side >= placed.side
            }
            .maxWithOrNull(largestThenRoomiest)?.first
    }

}
