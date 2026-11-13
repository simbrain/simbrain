/**
 * Decides where on its arrow a connector's drawing goes and how large it is, given what else is on the canvas. A
 * drawing is a square of adjustable size, such as a weight matrix image, with fixed parts around it, such as a label
 * box above and a caption below. A label box on its own is a drawing whose square has no size.
 *
 * The drawing is kept clear of layers, including the two the arrow joins, of the arrowhead, and of the drawings of
 * connectors placed before it. It goes as near to its preferred place on the arrow as it can at the size it would
 * have with the arrow to itself, sliding along the arrow to get clear, and only where no place on the arrow has room
 * for that size does it shrink. The square shrinks to keep itself clear, never to keep a label box wider than it
 * clear, which is left overlapping instead.
 *
 * Where it cannot be clear of everything, it is kept clear of layers first, since layers are drawn over it and
 * would hide it, and overlaps other drawings as little as it can.
 */
package org.simbrain.network.gui.nodes

import org.simbrain.util.p
import java.awt.geom.CubicCurve2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * What a connector draws on its arrow, as placement sees it.
 *
 * @param above height of what is stacked above the square, such as a label box
 * @param below height of what is stacked below the square, such as a caption
 * @param beside width of what is on each side of the square
 * @param minWidth width of the widest fixed part, such as a label box, which the drawing is never narrower than
 * @param minSide side of the square below which it is not shrunk, even where that leaves it overlapping
 * @param maxSide side of the square above which it is not grown
 * @param comfortable side the square may reach by filling the room it has outright, so that [fill] only holds back
 * a square larger than this
 * @param fill fraction of the arrow that the drawing may take up, which leaves the rest of the arrow showing
 */
internal class ShaftDrawing(
    val above: Double = 0.0,
    val below: Double = 0.0,
    val beside: Double = 0.0,
    val minWidth: Double = 0.0,
    val minSide: Double = 0.0,
    val maxSide: Double = 0.0,
    val comfortable: Double = 0.0,
    val fill: Double = 1.0
) {

    /** Half the width of the drawing when its square is [side] across. */
    fun halfWidth(side: Double) = max(side / 2 + beside, minWidth / 2)

    /** The space the drawing takes up when its square is [side] across and centered on ([x], [y]). */
    fun footprintAt(x: Double, y: Double, side: Double): Rectangle2D = Rectangle2D.Double(
        x - halfWidth(side), y - side / 2 - above, 2 * halfWidth(side), side + above + below
    )

    /** Whether the drawing has a square at all, rather than being a label box on its own. */
    val hasSquare get() = maxSide > 0.0

    /**
     * Side of the largest square centered on ([x], [y]) for which the drawing, with [margin] around it, stays clear
     * of [obstacle]. Negative when the drawing cannot be clear of it there at any size, and the more negative the
     * further it would have to move to get clear.
     *
     * @param squareOnly measure for the square and what is beside it alone, letting the widest fixed part overlap
     */
    fun roomBeside(x: Double, y: Double, obstacle: Rectangle2D, margin: Double, squareOnly: Boolean = false): Double {
        // Clear space to the nearer side of the obstacle, negative when the center is within its width
        val clearAcross = max(obstacle.x - x, x - obstacle.maxX) - margin
        val roomAcross = if (squareOnly || clearAcross >= minWidth / 2) {
            2 * (clearAcross - beside)
        } else {
            2 * (clearAcross - minWidth / 2)
        }
        // Clear space above an obstacle below the center or below one above it, whichever is nearer to enough
        val roomAlong = 2 * max(y - obstacle.maxY - above - margin, obstacle.y - y - below - margin)
        return max(roomAcross, roomAlong)
    }
}

/** A label box on its own, as placement on an arrow sees it: a drawing whose square has no size. */
internal fun InteractionBox.asShaftDrawing(): ShaftDrawing {
    val box = fullBounds
    return ShaftDrawing(above = box.height / 2, below = box.height / 2, minWidth = box.width)
}

/**
 * Where a drawing goes on its arrow.
 *
 * @param centerX where the center of the square goes
 * @param centerY where the center of the square goes
 * @param side side of the square
 * @param position how far along the shaft the center is, from 0 at the source layer to 1 at the base of the arrowhead
 */
internal class ShaftPlacement(val centerX: Double, val centerY: Double, val side: Double, val position: Double)

internal object ShaftPlacer {

    /** Space kept clear between a drawing and an obstacle. */
    const val MARGIN = 6.0

    /** How near either end of the shaft the center of a drawing may go. */
    private const val END_CLEARANCE = 0.08

    /** Distance along the shaft between the places first tried, which are then refined. */
    private const val SAMPLE_SPACING = 4.0

    private const val REFINEMENT_STEPS = 16

    /** A size this much short of the one wanted still counts as having room for it. */
    private const val SIZE_TOLERANCE = 0.01

    /** Length of an arrowhead, which starts where the shaft ends, and half its width. */
    private const val HEAD_LENGTH = 35.0
    private const val HEAD_HALF_WIDTH = 20.0

    private const val FAR = 1.0e7

    /**
     * Place [drawing] on [shaft], which runs from an edge of the source layer to the base of the arrowhead.
     *
     * @param layers the space taken up by each layer on the canvas, in canvas coordinates
     * @param drawings the space taken up by the drawings of the connectors placed before this one
     * @param ownLayers the two layers the arrow joins, which are obstacles as well. The label box of a drawing
     * with a square is let overlap them, as it must between two layers side by side that are closer together than
     * it is wide, where the square still has to fit between them
     * @param preferred where on the shaft the drawing goes when nothing is in the way, from 0 to 1
     * @param shared whether a connector running the other way draws in the same gap, in which case this one keeps to
     * the half nearer its source
     */
    fun place(
        shaft: CubicCurve2D,
        drawing: ShaftDrawing,
        layers: List<Rectangle2D>,
        drawings: List<Rectangle2D> = emptyList(),
        ownLayers: List<Rectangle2D> = emptyList(),
        preferred: Double = 0.5,
        shared: Boolean = false
    ): ShaftPlacement {
        val length = shaft.arcLength()
        val preferredPoint = shaft.p(preferred)
        if (length <= 0.0) return ShaftPlacement(preferredPoint.x, preferredPoint.y, drawing.minSide, preferred)

        // The arrow's own ends limit the drawing as its layers do
        val ends = ownLayers + listOfNotNull(arrowhead(shaft), farHalf(shaft).takeIf { shared })
        val otherLayers = layers.filter { layer -> ownLayers.none { it == layer } }
        // What the drawing has to have room for beside a layer before its room beside other drawings counts
        val leastSide = drawing.minSide

        // The size the drawing has with the arrow to itself. Along a slanted arrow the fixed parts take up less of
        // its length than their height or width
        val (alongX, alongY) = shaft.direction()
        val taken = abs(alongY) * (drawing.above + drawing.below) + abs(alongX) * 2 * drawing.beside
        val available = if (shared) length / 2 else length
        val wanted = max(drawing.comfortable, drawing.fill * available - taken).coerceIn(drawing.minSide, drawing.maxSide)

        // How far the drawing as a whole is from where it is preferred. With more above the square than below it,
        // the drawing as a whole is centered above the square
        fun distanceFromPreferred(position: Double): Double {
            val point = shaft.p(position)
            return hypot(point.x - preferredPoint.x, point.y + (drawing.below - drawing.above) / 2 - preferredPoint.y)
        }

        val samples = ceil(length / SAMPLE_SPACING).toInt().coerceIn(40, 400)
        val step = (1 - 2 * END_CLEARANCE) / samples
        val positions = List(samples + 1) { END_CLEARANCE + it * step }
        fun around(position: Double) = List(2 * REFINEMENT_STEPS + 1) {
            (position + (it - REFINEMENT_STEPS) * step / REFINEMENT_STEPS).coerceIn(END_CLEARANCE, 1 - END_CLEARANCE)
        }

        /**
         * The best place on the shaft, keeping either the whole drawing clear of the obstacles or, with
         * [squareOnly], only its square and what is beside it.
         */
        fun search(squareOnly: Boolean): ShaftPlacement {
            /** Room at [position] beside the layers and the arrow's own ends. */
            fun roomBesideLayers(position: Double): Double {
                val point = shaft.p(position)
                var room = Double.POSITIVE_INFINITY
                for (layer in otherLayers) {
                    room = min(room, drawing.roomBeside(point.x, point.y, layer, MARGIN, squareOnly))
                }
                for (end in ends) {
                    room = min(room, drawing.roomBeside(point.x, point.y, end, MARGIN, squareOnly = drawing.hasSquare))
                }
                return room
            }

            /** Room at [position] beside everything. */
            fun roomAt(position: Double): Double {
                val point = shaft.p(position)
                var room = roomBesideLayers(position)
                for (other in drawings) {
                    room = min(room, drawing.roomBeside(point.x, point.y, other, MARGIN, squareOnly))
                }
                return room
            }

            /** The position among [candidates] with room for the size wanted that is nearest the preferred one. */
            fun nearestWithRoom(candidates: List<Double>) =
                candidates.filter { roomAt(it) >= wanted - SIZE_TOLERANCE }.minByOrNull { distanceFromPreferred(it) }

            /**
             * The position among [candidates] that is clear of the layers, if any is, and of those the one with the
             * most room beside everything, and of those the nearest the preferred one.
             */
            fun roomiest(candidates: List<Double>) = candidates.maxWith(
                compareBy<Double> { min(roomBesideLayers(it), leastSide) }
                    .thenBy { roomAt(it) }
                    .thenBy { -distanceFromPreferred(it) }
            )

            val withRoom = nearestWithRoom(positions)
            val position: Double
            val side: Double
            if (withRoom != null) {
                // Room for the size wanted: slide no further than needed
                position = nearestWithRoom(around(withRoom)) ?: withRoom
                side = wanted
            } else {
                // No room for it anywhere: go where there is most, and shrink to that
                position = roomiest(around(roomiest(positions)))
                side = min(wanted, roomAt(position)).coerceAtLeast(drawing.minSide)
            }
            val center = shaft.p(position)
            return ShaftPlacement(center.x, center.y, side, position)
        }

        // A label box wider than the square is kept clear of what is around it by sliding, but the square is not
        // shrunk for its sake: a label box that overlaps its neighbor reads better than an image too small to see
        val allClear = search(squareOnly = false)
        if (!drawing.hasSquare || allClear.side >= wanted - SIZE_TOLERANCE) return allClear
        val squareClear = search(squareOnly = true)
        return if (squareClear.side > allClear.side + SIZE_TOLERANCE) squareClear else allClear
    }

    private fun CubicCurve2D.arcLength(): Double {
        var length = 0.0
        var previous = p(0.0)
        for (i in 1..32) {
            val next = p(i / 32.0)
            length += previous.distance(next)
            previous = next
        }
        return length
    }

    /** Unit vector from the start of the shaft to its end. */
    private fun CubicCurve2D.direction(): Pair<Double, Double> {
        val length = hypot(x2 - x1, y2 - y1)
        return if (length == 0.0) 0.0 to 0.0 else (x2 - x1) / length to (y2 - y1) / length
    }

    /** The space the arrowhead takes up beyond the end of the shaft. */
    private fun arrowhead(shaft: CubicCurve2D): Rectangle2D? {
        var headX = shaft.x2 - shaft.ctrlX2
        var headY = shaft.y2 - shaft.ctrlY2
        val length = hypot(headX, headY)
        if (length == 0.0) return null
        headX /= length
        headY /= length
        val tip = Point2D.Double(shaft.x2 + headX * HEAD_LENGTH, shaft.y2 + headY * HEAD_LENGTH)
        val across = HEAD_HALF_WIDTH
        return Rectangle2D.Double(
            min(shaft.x2, tip.x) - across * abs(headY),
            min(shaft.y2, tip.y) - across * abs(headX),
            abs(tip.x - shaft.x2) + 2 * across * abs(headY),
            abs(tip.y - shaft.y2) + 2 * across * abs(headX)
        )
    }

    /**
     * Everything on the far side of the middle of the shaft from its source, across the direction the shaft leaves
     * the source in.
     */
    private fun farHalf(shaft: CubicCurve2D): Rectangle2D {
        val outX = shaft.ctrlX1 - shaft.x1
        val outY = shaft.ctrlY1 - shaft.y1
        val middleX = (shaft.x1 + shaft.x2) / 2
        val middleY = (shaft.y1 + shaft.y2) / 2
        return if (abs(outX) > abs(outY)) {
            if (outX > 0) Rectangle2D.Double(middleX, -FAR, FAR, 2 * FAR) else Rectangle2D.Double(middleX - FAR, -FAR, FAR, 2 * FAR)
        } else {
            if (outY > 0) Rectangle2D.Double(-FAR, middleY, 2 * FAR, FAR) else Rectangle2D.Double(-FAR, middleY - FAR, 2 * FAR, FAR)
        }
    }
}
