/**
 * Decides where on its arrow a connector's drawing goes and how large it is, given what else is on the canvas. A
 * drawing is a square of adjustable size, such as a weight matrix image, with fixed parts around it, such as a label
 * box above and a caption below. A label box on its own is a drawing whose square has no size.
 *
 * With the arrow to itself, a drawing takes up a set fraction of the arrow's length, so that the rest of the arrow
 * shows on either side of it whatever the length.
 *
 * The drawing is kept clear of layers, including the two the arrow joins, of the arrowhead, and of the drawings of
 * connectors placed before it. It goes as near to its preferred place on the arrow as it can at the size it would
 * have with the arrow to itself, sliding along the arrow to get clear, and only where no place on the arrow has room
 * for that size does it shrink. The square shrinks to keep itself clear, never to keep a label box wider than it
 * clear, which is left overlapping instead.
 *
 * Where it cannot be clear of everything, it is kept clear of layers first, since layers are drawn over it and
 * would hide it, and overlaps other drawings as little as it can.
 *
 * Drawings that would collide if each were placed as though the others were not there are placed together instead:
 * all of them are moved the same way along their arrows, and if that is not enough the largest of them are shrunk
 * until they fit. Neither gives way to the other, so arrangements that mirror each other are drawn mirroring each
 * other.
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
 * @param fill fraction of the arrow that the drawing takes up, which leaves the rest of the arrow showing
 */
class ShaftDrawing(
    val above: Double = 0.0,
    val below: Double = 0.0,
    val beside: Double = 0.0,
    val minWidth: Double = 0.0,
    val minSide: Double = 0.0,
    val maxSide: Double = 0.0,
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

/**
 * A label box on its own, as placement on an arrow sees it: a drawing whose square has no size. It is placed for
 * its full size, whatever size it is drawn at.
 */
fun InteractionBox.asShaftDrawing() =
    ShaftDrawing(above = layoutHeight / 2, below = layoutHeight / 2, minWidth = layoutWidth)

/**
 * Where a drawing goes on its arrow.
 *
 * @param centerX where the center of the square goes
 * @param centerY where the center of the square goes
 * @param side side of the square
 * @param position how far along the shaft the center is, from 0 at the source layer to 1 at the base of the arrowhead
 * @param room side of the largest square there is room for at that place, whatever size the drawing would have. Less
 * than [side] where the square overlaps something even at its smallest, and negative where it would overlap at any
 * size
 */
class ShaftPlacement(
    val centerX: Double,
    val centerY: Double,
    val side: Double,
    val position: Double,
    val room: Double = side
)

/**
 * A connector's drawing waiting to be placed on its arrow among the drawings of other connectors.
 *
 * @param shaft the arrow's shaft, from an edge of the source layer to the base of the arrowhead
 * @param ownLayers the space taken up by the two layers the arrow joins, in canvas coordinates
 * @param preferred where on the shaft the drawing goes when nothing is in the way, from 0 to 1
 * @param shared whether a connector running the other way draws in the same gap, in which case this one keeps to the
 * half nearer its source
 * @param lead length of the arrow before the shaft, where it runs straight out from the source layer, which counts
 * toward the length the drawing is sized for
 * @param widest side the square is not grown beyond on this arrow, where a larger one would cover something that
 * runs beside the arrow
 */
class ShaftItem(
    val shaft: CubicCurve2D,
    val drawing: ShaftDrawing,
    val ownLayers: List<Rectangle2D> = emptyList(),
    val preferred: Double = 0.5,
    val shared: Boolean = false,
    val lead: Double = 0.0,
    val widest: Double = Double.POSITIVE_INFINITY
)

object ShaftPlacer {

    /** Space kept clear between a drawing and an obstacle. */
    const val MARGIN = 6.0

    /** How near either end of the shaft the center of a drawing may go. */
    private const val END_CLEARANCE = 0.08

    /**
     * How near the start of its shaft the center of a drawing may go: all the way where the arrow runs on before
     * the shaft, so that the start of the shaft is not the end of the arrow.
     */
    private fun nearestStart(lead: Double) = if (lead > 0.0) 0.0 else END_CLEARANCE

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
     * @param lead length of the arrow before the shaft, which counts toward the length the drawing is sized for
     * @param widest side the square is not grown beyond on this arrow
     */
    fun place(
        shaft: CubicCurve2D,
        drawing: ShaftDrawing,
        layers: List<Rectangle2D>,
        drawings: List<Rectangle2D> = emptyList(),
        ownLayers: List<Rectangle2D> = emptyList(),
        preferred: Double = 0.5,
        shared: Boolean = false,
        lead: Double = 0.0,
        widest: Double = Double.POSITIVE_INFINITY
    ): ShaftPlacement {
        val length = shaft.arcLength()
        val preferredPoint = shaft.p(preferred)
        if (length <= 0.0) return ShaftPlacement(preferredPoint.x, preferredPoint.y, drawing.minSide, preferred, 0.0)

        // The arrow's own ends limit the drawing as its layers do
        val ends = ownLayers + listOfNotNull(arrowhead(shaft), farHalf(shaft).takeIf { shared })
        val otherLayers = layers.filter { layer -> ownLayers.none { it == layer } }
        // What the drawing has to have room for beside a layer before its room beside other drawings counts
        val leastSide = drawing.minSide

        val wanted = wantedSide(shaft, drawing, lead, widest)

        // How far the drawing as a whole is from where it is preferred. With more above the square than below it,
        // the drawing as a whole is centered above the square
        fun distanceFromPreferred(position: Double): Double {
            val point = shaft.p(position)
            return hypot(point.x - preferredPoint.x, point.y + (drawing.below - drawing.above) / 2 - preferredPoint.y)
        }

        val samples = ceil(length / SAMPLE_SPACING).toInt().coerceIn(40, 400)
        val first = nearestStart(lead)
        val step = (1 - END_CLEARANCE - first) / samples
        val positions = List(samples + 1) { first + it * step }
        fun around(position: Double) = List(2 * REFINEMENT_STEPS + 1) {
            (position + (it - REFINEMENT_STEPS) * step / REFINEMENT_STEPS).coerceIn(first, 1 - END_CLEARANCE)
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
            return ShaftPlacement(center.x, center.y, side, position, roomAt(position))
        }

        // A label box wider than the square is kept clear of what is around it by sliding, but the square is not
        // shrunk for its sake: a label box that overlaps its neighbor reads better than an image too small to see
        val allClear = search(squareOnly = false)
        if (!drawing.hasSquare || allClear.side >= wanted - SIZE_TOLERANCE) return allClear
        val squareClear = search(squareOnly = true)
        return if (squareClear.side > allClear.side + SIZE_TOLERANCE) squareClear else allClear
    }

    /** Place [item] as [place] does. */
    fun place(item: ShaftItem, layers: List<Rectangle2D>, drawings: List<Rectangle2D> = emptyList()) =
        place(
            item.shaft, item.drawing, layers, drawings, item.ownLayers, item.preferred, item.shared, item.lead, item.widest
        )

    /** The size the square of [item] has with its arrow to itself. */
    fun wantedSide(item: ShaftItem) = wantedSide(item.shaft, item.drawing, item.lead, item.widest)

    /**
     * The size the square of [drawing] has with [shaft] to itself: what leaves the drawing as a whole covering its
     * fraction of the arrow's length, and no more than [widest]. Along a slanted arrow the fixed parts cover less of
     * its length than their height or width, and the square covers more of it than its side.
     */
    private fun wantedSide(shaft: CubicCurve2D, drawing: ShaftDrawing, lead: Double, widest: Double): Double {
        val length = shaft.arcLength() + lead
        val (alongX, alongY) = shaft.direction()
        val taken = abs(alongY) * (drawing.above + drawing.below) + abs(alongX) * 2 * drawing.beside
        val coveredBySquare = (abs(alongX) + abs(alongY)).coerceAtLeast(1.0)
        val largest = min(drawing.maxSide, widest).coerceAtLeast(drawing.minSide)
        return ((drawing.fill * length - taken) / coveredBySquare).coerceIn(drawing.minSide, largest)
    }

    /**
     * Place every one of [items] on its arrow. Each is placed as though the others were not there, and those that
     * would then collide are placed together by [placeTogether]. Where an item that collides with nothing still has
     * to keep clear of a group placed before it, the group keeps its place.
     *
     * @param layers the space taken up by each layer on the canvas, in canvas coordinates
     * @param fixed the space taken up by drawings that are not on a shaft and so do not move
     * @return where each item goes, in the order of [items]
     */
    fun placeAll(items: List<ShaftItem>, layers: List<Rectangle2D>, fixed: List<Rectangle2D> = emptyList()): List<ShaftPlacement> {
        val alone = items.map { place(it, layers, fixed) }
        val placed = fixed.toMutableList()
        val result = arrayOfNulls<ShaftPlacement>(items.size)
        for (group in collidingGroups(items, alone)) {
            val placements = if (group.size == 1) {
                listOf(place(items[group.single()], layers, placed))
            } else {
                placeTogether(group.map { items[it] }, group.map { alone[it] }, layers, placed)
            }
            group.forEachIndexed { n, index ->
                result[index] = placements[n]
                placed += items[index].footprint(placements[n])
            }
        }
        return result.map { it!! }
    }

    /** The space [this] takes up when placed as [placement] says. */
    fun ShaftItem.footprint(placement: ShaftPlacement) =
        drawing.footprintAt(placement.centerX, placement.centerY, placement.side)

    /**
     * The items grouped so that any two whose drawings would be within [MARGIN] of each other, placed as [alone]
     * says, are in the same group. Groups are in the order of their first items, as indices into [items].
     */
    private fun collidingGroups(items: List<ShaftItem>, alone: List<ShaftPlacement>): List<List<Int>> {
        val footprints = items.indices.map { items[it].footprint(alone[it]) }
        val groupOf = IntArray(items.size) { it }
        fun find(index: Int): Int {
            var root = index
            while (groupOf[root] != root) root = groupOf[root]
            return root
        }
        for (i in items.indices) {
            val grown = Rectangle2D.Double(
                footprints[i].x - MARGIN, footprints[i].y - MARGIN,
                footprints[i].width + 2 * MARGIN, footprints[i].height + 2 * MARGIN
            )
            for (j in i + 1 until items.size) {
                if (grown.intersects(footprints[j])) groupOf[max(find(i), find(j))] = min(find(i), find(j))
            }
        }
        return items.indices.groupBy { find(it) }.toSortedMap().values.toList()
    }

    /**
     * Place [items], which would collide if each went where [alone] says, so that neither gives way to the other.
     * All are moved the same distance along their shafts, as a fraction of the shaft, toward their sources or toward
     * their targets, as little as gets them clear of each other. Where no such move gets them clear at the sizes
     * they have alone, they go where the least has to be given up, and none is left larger than a size they all
     * share: the largest shrink first, and a drawing already smaller than that keeps its size.
     *
     * @param alone where each item goes with the others not there, in the order of [items]
     * @param layers the space taken up by each layer on the canvas, in canvas coordinates
     * @param drawings the space taken up by drawings already placed
     */
    fun placeTogether(
        items: List<ShaftItem>,
        alone: List<ShaftPlacement>,
        layers: List<Rectangle2D>,
        drawings: List<Rectangle2D> = emptyList()
    ): List<ShaftPlacement> {
        val ends = items.map { it.ownLayers + listOfNotNull(arrowhead(it.shaft), farHalf(it.shaft).takeIf { _ -> it.shared }) }
        val otherLayers = items.map { item -> layers.filter { layer -> item.ownLayers.none { it == layer } } }
        val largest = alone.maxOf { it.side }
        // What every drawing has to have room for beside a layer before its room beside other drawings counts
        val leastSide = items.filter { it.drawing.hasSquare }.maxOfOrNull { it.drawing.minSide } ?: 0.0

        fun positionsAt(shift: Double) = items.indices.map {
            (alone[it].position + shift).coerceIn(nearestStart(items[it].lead), 1 - END_CLEARANCE)
        }

        /** The side of item [index] when no drawing is larger than [limit]. */
        fun sideWithin(limit: Double, index: Int) = min(alone[index].side, limit)

        /**
         * The limit that [room] for one drawing puts on the size of them all: none, if it has room for the size it
         * has alone, and otherwise the room it has, which is negative where it is not clear at any size.
         */
        fun limitFrom(room: Double, side: Double) = if (room >= side) Double.POSITIVE_INFINITY else room

        /**
         * The largest size that items [i] and [j] may both be limited to and still be clear of each other at
         * [points], or a negative measure of how far apart they would have to move when they are not clear at any.
         */
        fun limitBetween(i: Int, j: Int, points: List<Point2D>, squareOnly: Boolean): Double {
            fun halfWidth(index: Int, side: Double): Double {
                val drawing = items[index].drawing
                return if (squareOnly && drawing.hasSquare) side / 2 + drawing.beside else drawing.halfWidth(side)
            }
            val (upper, lower) = if (points[i].y <= points[j].y) i to j else j to i
            fun clearance(limit: Double): Double {
                val across = abs(points[i].x - points[j].x) - MARGIN -
                    halfWidth(i, sideWithin(limit, i)) - halfWidth(j, sideWithin(limit, j))
                val along = (points[lower].y - sideWithin(limit, lower) / 2 - items[lower].drawing.above) -
                    (points[upper].y + sideWithin(limit, upper) / 2 + items[upper].drawing.below) - MARGIN
                return max(across, along)
            }
            val touching = clearance(0.0)
            if (touching < 0.0) return touching
            var colliding = max(alone[i].side, alone[j].side)
            if (clearance(colliding) >= 0.0) return Double.POSITIVE_INFINITY
            var clear = 0.0
            repeat(24) {
                val middle = (clear + colliding) / 2
                if (clearance(middle) >= 0.0) clear = middle else colliding = middle
            }
            return clear
        }

        /** The size the drawings may be limited to at [shift]: beside the layers alone, and beside everything. */
        fun limitsAt(shift: Double, squareOnly: Boolean): Pair<Double, Double> {
            val points = positionsAt(shift).mapIndexed { index, position -> items[index].shaft.p(position) }
            var besideLayers = Double.POSITIVE_INFINITY
            for (i in items.indices) {
                val drawing = items[i].drawing
                val x = points[i].x
                val y = points[i].y
                for (layer in otherLayers[i]) {
                    besideLayers = min(besideLayers, limitFrom(drawing.roomBeside(x, y, layer, MARGIN, squareOnly), alone[i].side))
                }
                for (end in ends[i]) {
                    val room = drawing.roomBeside(x, y, end, MARGIN, squareOnly = drawing.hasSquare)
                    besideLayers = min(besideLayers, limitFrom(room, alone[i].side))
                }
            }
            var besideAll = besideLayers
            for (i in items.indices) {
                val drawing = items[i].drawing
                for (other in drawings) {
                    val room = drawing.roomBeside(points[i].x, points[i].y, other, MARGIN, squareOnly)
                    besideAll = min(besideAll, limitFrom(room, alone[i].side))
                }
                for (j in i + 1 until items.size) besideAll = min(besideAll, limitBetween(i, j, points, squareOnly))
            }
            return besideLayers to besideAll
        }

        val longest = items.maxOf { it.shaft.arcLength() }
        val samples = ceil(longest / SAMPLE_SPACING).toInt().coerceIn(40, 400)
        val step = (1 - 2 * END_CLEARANCE) / samples
        // Nearest first, and toward the sources before toward the targets
        val shifts = listOf(0.0) + (1..samples).flatMap { listOf(-it * step, it * step) }
        fun around(shift: Double) = List(2 * REFINEMENT_STEPS + 1) { shift + (it - REFINEMENT_STEPS) * step / REFINEMENT_STEPS }

        /** The shift to make and the size to limit to, keeping whole drawings clear or only their squares. */
        fun search(squareOnly: Boolean): Pair<Double, Double> {
            val measured = HashMap<Double, Pair<Double, Double>>()
            fun limits(shift: Double) = measured.getOrPut(shift) { limitsAt(shift, squareOnly) }
            fun fits(shift: Double) = limits(shift).second >= largest - SIZE_TOLERANCE
            val roomiest = compareBy<Double> { min(limits(it).first, leastSide) }
                .thenBy { limits(it).second }
                .thenBy { -abs(it) }
            val fitting = shifts.firstOrNull { fits(it) }
            val shift = if (fitting != null) {
                around(fitting).filter { fits(it) }.minByOrNull { abs(it) } ?: fitting
            } else {
                around(shifts.maxWith(roomiest)).maxWith(roomiest)
            }
            return shift to limits(shift).second
        }

        // As for a drawing on its own: a label box is kept clear by moving, but nothing is shrunk for its sake
        var (shift, limit) = search(squareOnly = false)
        if (limit < largest - SIZE_TOLERANCE && items.any { it.drawing.hasSquare }) {
            val squares = search(squareOnly = true)
            if (min(squares.second, largest) > limit + SIZE_TOLERANCE) {
                shift = squares.first
                limit = squares.second
            }
        }

        val positions = positionsAt(shift)
        return items.indices.map { index ->
            val item = items[index]
            val center = item.shaft.p(positions[index])
            val side = if (item.drawing.hasSquare) sideWithin(limit, index).coerceAtLeast(item.drawing.minSide) else 0.0
            ShaftPlacement(center.x, center.y, side, positions[index], min(alone[index].room, limit))
        }
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
