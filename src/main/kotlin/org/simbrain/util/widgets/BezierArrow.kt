/**
 * Curved arrows between two rectangular outlines, as connectors draw them between layers: which sides of the
 * outlines an arrow joins, the curve it takes between them, how it gets around what is in its way, and how two
 * arrows between the same pair of outlines, one each way, are kept from crossing.
 */
package org.simbrain.util.widgets

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.CubicCurve2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a Bezier curved with a single arrow at its end. Where it is located on its
 * source can be set, but it's target location is automatically determined.
 *
 * Terminology: a source and target [LocatableModel] are connected by a directed bezier
 * curve with a tail and tip.
 *
 * @param thickness thickness of the arrow in pixels
 * @param color color the arrow
 * @param lateralOffset where on the source edge the tail of the arrow is located. Starting is 0 and end is 1.
 *
 * @author Zoë Tosi
 * @author Leo Yulin Li
 */
class BezierArrow(template: BezierArrowTemplate) : PNode() {

    private val thickness = template.thickness
    private var color: Color = template.color
    private val headPadding = template.padding.head
    private val tailPadding = template.padding.tail
    private val tailBesideAnother = template.padding.tailBesideAnother
    private val _lateralOffset = template.lateralOffset
    private val lateralOffset get() = _lateralOffset()
    private val _lateralShift = template.lateralShift
    private val updateEvent = template.updateEvent

    /**
     * The triangle at the tip of the arrow. This shape is constructed only once, and during [layout] it is
     * transformed onto the correct location and unioned with the stroked curve.
     */
    private val arrowTipShape = listOf(point(0.0, -sin60deg), point(0.5, 0.0), point(-0.5, 0.0))
            .map { it * (thickness * 2.0) }.toPolygon()

    /**
     * The arrow as currently rendered: the stroked curve and the tip triangle unioned into a single
     * translucent shape, so their overlap does not double-blend into seams.
     */
    private var arrowView: PPath? = null

    /**
     * Update the shape of the arrow base on the outlines of source and target.
     *
     * @param blockers what the arrow should not pass through, in the coordinates of the outlines. An arrow whose
     * usual way passes through one of them goes around it instead
     * @return the shaft of the arrow, from the edge of the source to the base of the arrowhead, or null if the two
     * are placed so that no arrow can be drawn between them
     */
    fun layout(
        sourceOutlines: RectangleSides,
        targetOutlines: RectangleSides,
        bidirectional: Boolean,
        blockers: List<Rectangle2D> = emptyList()
    ): CubicCurve2D? {

        // 0. clear old arrow
        removeAllChildren()

        // 1. find the way the arrow runs: the one asked for, if it can be taken, and otherwise the usual one
        val plan = route?.let { planFor(it, sourceOutlines, targetOutlines) }
            ?: usualPlan(sourceOutlines, targetOutlines, bidirectional)
        if (plan == null) {
            shaft = null
            isDetour = false
            hasOwnLane = false
            lead = 0.0
            updateEvent(null)
            return null
        }

        // 2. go around anything in that way
        val inTheWay = blockers.filter { plan.curve.passesThrough(it) }
        val detour = if (inTheWay.isEmpty()) null else detourAround(inTheWay, sourceOutlines, targetOutlines, _lateralShift())
        val curveModel = detour?.first ?: plan.curve
        val headSide = detour?.second ?: plan.headSide
        isDetour = detour != null
        hasOwnLane = detour != null || plan.hasOwnLane
        shaft = curveModel
        val leadFrom = plan.leadFrom.takeIf { detour == null }
        lead = leadFrom?.distance(curveModel.p1) ?: 0.0

        // 3. transform the tip onto the head location and union it with the stroked curve
        val tipTransform = AffineTransform().apply {
            translate(curveModel.x2, curveModel.y2)
            rotate(headSide.normalTheta)
        }
        val drawn = Path2D.Double().apply {
            if (leadFrom != null) {
                moveTo(leadFrom.x, leadFrom.y)
                lineTo(curveModel.x1, curveModel.y1)
            } else {
                moveTo(curveModel.x1, curveModel.y1)
            }
            curveTo(curveModel.ctrlX1, curveModel.ctrlY1, curveModel.ctrlX2, curveModel.ctrlY2, curveModel.x2, curveModel.y2)
        }
        val arrowShape = Area(BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(drawn)).apply {
            add(Area(tipTransform.createTransformedShape(arrowTipShape)))
        }

        // 4. create the arrow PNode
        arrowView = PPath.Double(arrowShape, null).apply {
            paint = color
            transparency = 0.5f
        }.also { addChild(it) }

        // 5. call back
        updateEvent(curveModel)

        return curveModel
    }

    /**
     * Recolor the arrow to the given color.
     */
    fun updateColor(color: Color) {
        this.color = color
        arrowView?.paint = color
    }

    /**
     * Update the arrow color from the current [NetworkPreferences.connectorArrowColor].
     */
    fun updateColorFromPreferences() {
        updateColor(NetworkPreferences.connectorArrowColor)
    }

    /**
     * The sides the arrow is to join when it is next laid out, or null for the sides it joins by itself. Ignored
     * while the two outlines are placed so that those sides cannot be joined.
     */
    var route: ArrowRoute? = null

    /**
     * A way for the arrow to run between two outlines.
     *
     * @param leadFrom where the arrow is drawn straight from before its curve starts, or null where it starts with
     * its curve
     */
    private class Plan(
        val curve: CubicCurve2D,
        val headSide: Line2D,
        val route: ArrowRoute,
        val hasOwnLane: Boolean,
        val leadFrom: Point2D? = null
    ) {
        val lead get() = leadFrom?.distance(curve.p1) ?: 0.0
    }

    /**
     * Where an arrow between two sides attaches.
     *
     * @param tail where the arrow is drawn from, beside the source
     * @param start where its curve starts, which is further out than [tail] for an arrow that runs straight at first
     * @param head where its curve ends, at the base of the arrowhead
     */
    private class Ends(val tail: Point2D, val start: Point2D, val head: Point2D) {
        val leadFrom get() = tail.takeIf { it.distance(start) > 0.0 }
    }

    /**
     * Where the arrow attaches between [sourceSide] and [targetSide].
     *
     * An arrow that runs beside one the other way is laid out so that the two are the same curve side by side, which
     * keeps them from crossing. Its curve starts as far out from the source as the curve of the other arrow ends,
     * and the arrow runs straight up to there. And where the two sides face each other but are far enough apart
     * sideways that the curves would slant into each other between the layers, both arrows attach nearer each other's
     * layer along the sides, each layer giving up the same distance for as long as its side has room.
     */
    private fun endsBetween(sourceSide: Line2D, targetSide: Line2D, requestedShift: Double): Ends {
        val shift = shiftFor(sourceSide, targetSide, requestedShift)
        val tail = sourceSide.tailOffset(shift)
        val head = targetSide.headOffset(shift)
        if (requestedShift <= 0.0) return Ends(tail, tail, head)
        val outward = sourceSide.unitNormal

        fun endsWith(lead: Double): Ends {
            val run = ((head - tail) dot outward) - lead
            val (sourceMove, targetMove) = movesNearer(sourceSide, targetSide, tail, head, shift, run)
            val from = tail + sourceMove
            return Ends(from, from + outward * lead, head + targetMove)
        }

        val lead = ((tailBesideAnother ?: tailPadding) - tailPadding).coerceAtLeast(0.0)
        if (lead > 0.0) {
            val ends = endsWith(lead)
            val line = ends.head - ends.start
            // Between layers too close for the curve to start further out, it starts where the arrow does
            if ((line dot outward) >= SHORTEST_CURVE_BESIDE_ANOTHER && (line.norm dot outward) > 0.3) return ends
        }
        return endsWith(0.0)
    }

    /**
     * How far along their sides the two ends of an arrow that runs beside another move toward each other, as the
     * move of the end on the source and the move of the end on the target. Nothing where the sides do not face each
     * other, or where the arrow already goes no further across than [STEEPEST_BESIDE_ANOTHER] allows for its [run]
     * out from the source.
     */
    private fun movesNearer(
        sourceSide: Line2D,
        targetSide: Line2D,
        tail: Point2D,
        head: Point2D,
        shift: Double,
        run: Double
    ): Pair<Point2D, Point2D> {
        val none = point(0.0, 0.0) to point(0.0, 0.0)
        val sourceLength = sourceSide.p1.distance(sourceSide.p2)
        if (sourceLength == 0.0 || (sourceSide.unitNormal dot targetSide.unitNormal) > -0.9) return none
        val along = (sourceSide.p2 - sourceSide.p1) / sourceLength
        val across = (head - tail) dot along
        val excess = abs(across) - STEEPEST_BESIDE_ANOTHER * max(run, 0.0)
        if (excess <= 0.0) return none
        val toward = along * (if (across > 0) 1.0 else -1.0)

        /** How far the two arrows can move along [side] in [direction] from where they are centered on it. */
        fun travel(side: Line2D, center: Point2D, direction: Point2D) =
            (max((side.p1 - center) dot direction, (side.p2 - center) dot direction) - shift - thickness)
                .coerceAtLeast(0.0)
        val sourceTravel = travel(sourceSide, sourceSide.p(lateralOffset), toward)
        val targetTravel = travel(targetSide, targetSide.p(1 - lateralOffset), -toward)
        // Half each, and what one side has no room for goes to the other
        var sourceMove = min(excess / 2, sourceTravel)
        var targetMove = min(excess / 2, targetTravel)
        sourceMove = min(excess - targetMove, sourceTravel)
        targetMove = min(excess - sourceMove, targetTravel)
        return toward * sourceMove to toward * -targetMove
    }

    /**
     * Whether an arrow between these ends goes further across than [STEEPEST_BESIDE_ANOTHER] allows for the
     * distance it runs out from [sourceSide], so that two such arrows side by side would run into each other.
     */
    private fun Ends.slantsFrom(sourceSide: Line2D): Boolean {
        val line = head - start
        val outward = sourceSide.unitNormal
        val run = line dot outward
        val across = (line - outward * run).magnitude
        return across > STEEPEST_BESIDE_ANOTHER * run + 0.5
    }

    /**
     * A way for one of two arrows between the same layers to turn a corner: it leaves one side of its source and
     * arrives at a side of its target at a right angle to that one, bulging to the right of its direction. The
     * arrow the other way does the same, which takes it round the opposite corner, so that the two go round the
     * space between the layers and do not meet. Null where either has no room to turn.
     */
    private fun turnBesideAnother(sourceOutlines: RectangleSides, targetOutlines: RectangleSides): Plan? {
        val (ends, sourceSide, targetSide) = rightTurn(sourceOutlines, targetOutlines) ?: return null
        rightTurn(targetOutlines, sourceOutlines) ?: return null
        val sides = ArrowRoute(sourceOutlines.toList().indexOf(sourceSide), targetOutlines.toList().indexOf(targetSide))
        return Plan(curveBetween(ends, sourceSide, targetSide), targetSide, sides, hasOwnLane = true)
    }

    /**
     * The sides an arrow [from] one outline [to] another joins to turn a corner on the right of its direction, with
     * where it attaches to them, or null where it has no room to turn.
     */
    private fun rightTurn(from: RectangleSides, to: RectangleSides): Triple<Ends, Line2D, Line2D>? =
        (from.toList() cartesianProduct to.toList())
            .filter { (source, target) -> abs(source.unitNormal dot target.unitNormal) < 0.1 }
            .map { (source, target) ->
                val tail = source.tailOffset(0.0)
                Triple(Ends(tail, tail, target.headOffset(0.0)), source, target)
            }
            .firstOrNull { (ends, source, target) ->
                val line = ends.head - ends.tail
                val out = line dot source.unitNormal
                val into = -(line dot target.unitNormal)
                // Where the arrow would turn if it ran straight out of its source and straight in to its target
                val corner = ends.tail + source.unitNormal * out
                val toTheRight = point(-line.y, line.x)
                out >= ROOM_TO_TURN && into >= ROOM_TO_TURN &&
                    ((corner - (ends.tail + ends.head) / 2.0) dot toTheRight) > 0.0
            }

    /** The curve between [ends], which leaves [sourceSide] and arrives at [targetSide] at a right angle. */
    private fun curveBetween(ends: Ends, sourceSide: Line2D, targetSide: Line2D): CubicCurve2D {
        val delta = ends.head - ends.start
        return cubicBezier(
                ends.start,
                ends.start + delta.abs * sourceSide.unitNormal * 0.5,
                ends.head + delta.abs * targetSide.unitNormal * 0.5,
                ends.head
        )
    }

    /**
     * The way the arrow runs when no sides are asked for: between the nearest pair of sides that face each other.
     *
     * One of two arrows between the same layers runs beside the other, between the sides that face each other
     * that it leaves most squarely. Where the layers are so far apart sideways that the two would slant into each
     * other on the way, each turns a corner instead, as [turnBesideAnother] has it.
     */
    private fun usualPlan(sourceOutlines: RectangleSides, targetOutlines: RectangleSides, bidirectional: Boolean): Plan? {
        val requestedShift = _lateralShift()
        val candidates = (sourceOutlines.toList() cartesianProduct targetOutlines.toList())
                .map { (source, target) -> Triple(endsBetween(source, target, requestedShift), source, target) }
                .filter { (ends, source, target) ->
                    // make sure the curve does not bend backward
                    val line = ends.head - ends.start
                    line.norm dot source.unitNormal > 0.3 && line.norm dot target.unitNormal < -0.3
                }
        val (ends, sourceSide, targetSide) = if (bidirectional) {
            val squarest = compareBy<Triple<Ends, Line2D, Line2D>> { (ends, source, _) ->
                (ends.head - ends.start) dot source.normal
            }
            val facing = candidates
                .filter { (_, source, target) -> (source.unitNormal dot target.unitNormal) < -0.9 }
                .maxWithOrNull(squarest)
            if (facing == null || facing.first.slantsFrom(facing.second)) {
                turnBesideAnother(sourceOutlines, targetOutlines)?.let { return it }
            }
            facing ?: candidates.maxWithOrNull(squarest)
        } else {
            candidates.minByOrNull { (_, source, target) -> source.midPoint distanceSqTo target.midPoint }
        } ?: return null
        val sides = ArrowRoute(sourceOutlines.toList().indexOf(sourceSide), targetOutlines.toList().indexOf(targetSide))
        val shift = shiftFor(sourceSide, targetSide, requestedShift)
        // Two arrows that should run side by side cannot where one of the sides they join is too short for both
        if (shift < requestedShift - 0.5) {
            aroundTheCorner(sourceSide, targetSide, sourceOutlines, targetOutlines)?.let { (curve, arrivalSide) ->
                return Plan(curve, arrivalSide, sides, hasOwnLane = true)
            }
        }
        return Plan(curveBetween(ends, sourceSide, targetSide), targetSide, sides, hasOwnLane = false, ends.leadFrom)
    }

    /**
     * The way the arrow runs between the sides [route] names, or null if they cannot be joined as the outlines are
     * placed. Between the same side of each outline, the arrow bows outward from that side, far enough out to carry
     * what is drawn on it clear of both.
     */
    private fun planFor(route: ArrowRoute, sourceOutlines: RectangleSides, targetOutlines: RectangleSides): Plan? {
        val sourceSide = sourceOutlines.toList()[route.sourceSide]
        val targetSide = targetOutlines.toList()[route.targetSide]
        if (route.sourceSide == route.targetSide) {
            val between = (targetOutlines.center - sourceOutlines.center).norm
            if (abs(sourceSide.unitNormal dot between) >= 0.5) return null
            val tail = sourceSide.tailOffset(0.0)
            val head = targetSide.headOffset(0.0)
            val outward = sourceSide.unitNormal
            val reach = (sourceOutlines.corners() + targetOutlines.corners()).maxOf { it dot outward } + DETOUR_CLEARANCE
            val start = ((tail dot outward) + (head dot outward)) / 2
            val bow = max(MINIMUM_BOW, (reach - start) / 0.75)
            val curve = cubicBezier(tail, tail + outward * bow, head + outward * bow, head)
            return Plan(curve, targetSide, route, hasOwnLane = true)
        }
        val ends = endsBetween(sourceSide, targetSide, _lateralShift())
        val delta = ends.head - ends.start
        val bendsBackward = !(delta.norm dot sourceSide.unitNormal > 0.3 && delta.norm dot targetSide.unitNormal < -0.3)
        if (bendsBackward) return null
        return Plan(curveBetween(ends, sourceSide, targetSide), targetSide, route, hasOwnLane = false, ends.leadFrom)
    }

    /**
     * The shaft the arrow would have between the sides [route] names, or between the sides it joins by itself for
     * null, without laying it out. Null if no arrow can be drawn that way.
     */
    fun shaftFor(
        route: ArrowRoute?,
        sourceOutlines: RectangleSides,
        targetOutlines: RectangleSides,
        bidirectional: Boolean
    ): RoutedShaft? {
        val plan = if (route == null) usualPlan(sourceOutlines, targetOutlines, bidirectional) else planFor(route, sourceOutlines, targetOutlines)
        return plan?.let { RoutedShaft(it.curve, it.hasOwnLane, it.lead) }
    }

    /**
     * The other sides the arrow could join. One of two arrows between the same layers has a single other way: a bow
     * on the side to the right of its direction, so that the two bow on opposite sides. An arrow on its own may join
     * any other pair of sides that face each other, or bow on either side.
     */
    fun otherRoutes(sourceOutlines: RectangleSides, targetOutlines: RectangleSides, bidirectional: Boolean): List<ArrowRoute> {
        val usual = usualPlan(sourceOutlines, targetOutlines, bidirectional)?.route
        val sources = sourceOutlines.toList()
        val between = (targetOutlines.center - sourceOutlines.center).norm
        val across = sources.indices.filter { abs(sources[it].unitNormal dot between) < 0.5 }
        val bows = if (bidirectional) {
            val right = point(-between.y, between.x)
            listOfNotNull(across.maxByOrNull { sources[it].unitNormal dot right })
        } else {
            across
        }.map { ArrowRoute(it, it) }
        val facing = if (bidirectional) {
            emptyList()
        } else {
            sources.indices.flatMap { source -> sources.indices.map { target -> ArrowRoute(source, target) } }
                .filter { it.sourceSide != it.targetSide && planFor(it, sourceOutlines, targetOutlines) != null }
        }
        return (facing + bows).filter { it != usual }
    }

    private fun RectangleSides.corners() = listOf(top.p1, top.p2, bottom.p1, bottom.p2)

    /** The shaft of the arrow as last laid out, or null before then or where no arrow could be drawn. */
    var shaft: CubicCurve2D? = null
        private set

    /** How far the arrow as last laid out runs straight out from its source before its [shaft] starts. */
    var lead = 0.0
        private set

    /** Whether the arrow as last laid out goes around something that was in its way. */
    var isDetour = false
        private set

    /**
     * Whether the arrow as last laid out runs clear of the arrow that runs the other way between the same two
     * layers, rather than beside it in the gap between them: it goes around something, or attaches around the
     * corner from a side too short for both.
     */
    var hasOwnLane = false
        private set

    /**
     * Whether the arrow would be laid out differently among [blockers] than it last was: it goes around something,
     * which may have moved, or something is now in its way.
     */
    fun affectedBy(blockers: List<Rectangle2D>) = isDetour || (shaft?.let { curve -> blockers.any { curve.passesThrough(it) } } ?: false)

    private fun CubicCurve2D.passesThrough(blocker: Rectangle2D) =
        (0..SAMPLES_ALONG_CURVE).any { blocker.contains(p(it.toDouble() / SAMPLES_ALONG_CURVE)) }

    /**
     * A way from the source to the target around [inTheWay], which the usual way passes through. The arrow leaves
     * one side of the source and arrives at the same side of the target, bowing outward far enough to pass what is
     * in the way with room to spare for what is drawn on the arrow. Of the sides that face across the line from the
     * source to the target, it takes the one that needs the smaller bow.
     *
     * @return the curve and the side of the target it arrives at
     */
    private fun detourAround(
        inTheWay: List<Rectangle2D>,
        sourceOutlines: RectangleSides,
        targetOutlines: RectangleSides,
        requestedShift: Double
    ): Pair<CubicCurve2D, Line2D>? {
        val between = (targetOutlines.center - sourceOutlines.center).norm
        return sourceOutlines.toList().zip(targetOutlines.toList())
            .filter { (sourceSide, _) -> abs(sourceSide.unitNormal dot between) < 0.5 }
            .map { (sourceSide, targetSide) ->
                val shift = shiftFor(sourceSide, targetSide, requestedShift)
                val tail = sourceSide.tailOffset(shift)
                val head = targetSide.headOffset(shift)
                val outward = sourceSide.unitNormal
                // How far out what is in the way reaches on this side, and how far out the ends of the arrow are
                val reach = inTheWay.maxOf { blocker -> blocker.corners().maxOf { it dot outward } } + DETOUR_CLEARANCE
                val start = ((tail dot outward) + (head dot outward)) / 2
                // The middle of a curve bowed by a distance is three quarters of that distance out
                val bow = max(MINIMUM_BOW, (reach - start) / 0.75)
                Triple(bow, cubicBezier(tail, tail + outward * bow, head + outward * bow, head), targetSide)
            }
            .minByOrNull { it.first }
            ?.let { it.second to it.third }
    }

    /**
     * A way between two sides of which one is too short for this arrow and the one that runs the other way to
     * attach to it side by side. This arrow attaches around the corner from the short side instead, on the flank of
     * that layer nearer its own end of the long side, and the other arrow takes the opposite flank, so that the two
     * pass either side of the narrow layer. Null where the long side is not long enough to reach past the narrow
     * layer.
     *
     * @return the curve and the side of the target it arrives at
     */
    private fun aroundTheCorner(
        sourceSide: Line2D,
        targetSide: Line2D,
        sourceOutlines: RectangleSides,
        targetOutlines: RectangleSides
    ): Pair<CubicCurve2D, Line2D>? {
        fun Line2D.length() = p1.distance(p2)
        val targetIsNarrow = targetSide.length() < sourceSide.length()
        val longSide = if (targetIsNarrow) sourceSide else targetSide
        val shortSide = if (targetIsNarrow) targetSide else sourceSide
        val narrowOutlines = if (targetIsNarrow) targetOutlines else sourceOutlines
        // Far enough along the long side to come up beside the narrow layer and turn in to its flank
        val endPadding = if (targetIsNarrow) headPadding else tailPadding
        val reach = shortSide.length() / 2 + endPadding + ROOM_TO_TURN
        val shift = reach.coerceAtMost(longSide.length() / 2 - thickness / 2)
        if (shift < shortSide.length() / 2 + thickness) return null
        val fraction = 0.5 - shift / longSide.length()
        val onLongSide = if (targetIsNarrow) {
            longSide.p(fraction) + longSide.unitNormal * tailPadding
        } else {
            longSide.p(1 - fraction) + longSide.unitNormal * headPadding
        }
        val flank = narrowOutlines.toList()
            .filter { abs(it.unitNormal dot shortSide.unitNormal) < 0.5 }
            .minByOrNull { it.midPoint distanceSqTo onLongSide } ?: return null
        val onFlank = flank.midPoint + flank.unitNormal * endPadding
        val (tail, tailSide) = if (targetIsNarrow) onLongSide to longSide else onFlank to flank
        val (head, arrivalSide) = if (targetIsNarrow) onFlank to flank else onLongSide to longSide
        val delta = (head - tail).abs
        // Out from each side far enough to turn the corner without a kink
        fun outFrom(side: Line2D) = side.unitNormal * max(ROOM_TO_TURN, (delta * side.unitNormal.abs).let { it.x + it.y } * 0.5)
        return cubicBezier(tail, tail + outFrom(tailSide), head + outFrom(arrivalSide), head) to arrivalSide
    }

    private val RectangleSides.center
        get() = point((left.midPoint.x + right.midPoint.x) / 2, (top.midPoint.y + bottom.midPoint.y) / 2)

    private fun Rectangle2D.corners() = listOf(point(x, y), point(maxX, y), point(x, maxY), point(maxX, maxY))

    /**
     * How far from the middle of the sides it joins the arrow attaches, given how far it was asked to. The same at
     * both ends, so that the arrow runs parallel to the line between the middles of the two sides, and no further
     * than leaves the arrow on the shorter of the two.
     */
    private fun shiftFor(sourceSide: Line2D, targetSide: Line2D, requested: Double): Double {
        val shorter = min(sourceSide.p1.distance(sourceSide.p2), targetSide.p1.distance(targetSide.p2))
        return requested.coerceAtMost(shorter / 2 - thickness / 2).coerceAtLeast(0.0)
    }

    /** How far along a side an arrow shifted by [shift] from the middle attaches, as a fraction of the side. */
    private fun Line2D.fractionFor(shift: Double): Double {
        val length = p1.distance(p2)
        return if (length == 0.0) lateralOffset else lateralOffset - shift / length
    }

    /**
     * Given a side of a rectangle bound, find the location of where an arrow tail would go.
     */
    private fun Line2D.tailOffset(shift: Double) = p(fractionFor(shift)) + (unitNormal * tailPadding)

    /**
     * Given a side of a rectangle bound, find the location of where an arrow head would go.
     */
    private fun Line2D.headOffset(shift: Double) = p(1 - fractionFor(shift)) + (unitNormal * headPadding)
}

@DslMarker
annotation class BezierArrowMaker

@BezierArrowMaker
class BezierArrowTemplate {

    var color: Color = NetworkPreferences.connectorArrowColor

    var thickness = 20.0f

    val padding = PaddingBuilder()

    var updateEvent: (CubicCurve2D?) -> Unit = {}
        private set

    var lateralOffset: () -> Double = { 0.5 }
        private set

    var lateralShift: () -> Double = { 0.0 }
        private set

    @BezierArrowMaker
    inner class PaddingBuilder {
        operator fun invoke(init: PaddingBuilder.() -> Unit) {
            init()
        }

        val default get() = 35.0
        val arrowSize get() = this@BezierArrowTemplate.thickness * 2 * sin60deg
        val defaultHead get() = default + arrowSize
        val defaultTail get() = default
        var head = defaultHead
        var tail = defaultTail

        /**
         * How far from its source the curve of the arrow starts when it runs beside an arrow the other way. Set to
         * how far from that layer the curve of the other arrow ends, which is its head padding, the two arrows are
         * the same curve side by side and do not cross. The arrow is drawn straight from [tail] up to there. Null
         * for a curve that starts at [tail] regardless.
         */
        var tailBesideAnother: Double? = null
    }

    /** Where along a side the arrow attaches, as a fraction of the side. */
    fun lateralOffset(block: () -> Double) {
        lateralOffset = block
    }

    /**
     * How far to one side of where [lateralOffset] puts it the arrow attaches, in the units of the canvas. Unlike a
     * fraction, a distance is the same on a long side and a short one, so two arrows shifted by the same distance
     * between the same two layers, one each way, run parallel whatever the sizes of the layers.
     */
    fun lateralShift(block: () -> Double) {
        lateralShift = block
    }

    fun onUpdated(block: (CubicCurve2D?) -> Unit) {
        updateEvent = block
    }
}

/**
 * A pair of sides for an arrow to join: one of the source and one of the target, each counted from the top round by
 * the right, the bottom and the left.
 */
data class ArrowRoute(val sourceSide: Int, val targetSide: Int)

/**
 * A shaft an arrow could have.
 *
 * @param hasOwnLane whether an arrow on it runs clear of an arrow the other way between the same two layers
 * @param lead how far the arrow runs straight out from its source before the shaft starts
 */
class RoutedShaft(val curve: CubicCurve2D, val hasOwnLane: Boolean, val lead: Double = 0.0) {

    /**
     * How far along the curve the middle of the half of the arrow nearer its source is, counting the [lead] before
     * the curve and the arrowhead after it, which is about as long.
     */
    val middleOfNearHalf: Double
        get() {
            val length = curve.p1.distance(curve.p2)
            return if (length == 0.0) 0.25 else (0.25 - 0.5 * lead / length).coerceIn(0.0, 1.0)
        }
}

/** Space left between an arrow that goes around something and what it goes around, for what is drawn on the arrow. */
private const val DETOUR_CLEARANCE = 80.0

/** The least an arrow that goes around something bows out. */
private const val MINIMUM_BOW = 60.0

/**
 * Room an arrow that turns a corner has to turn in: how far out it runs from the side it leaves, and how far in to
 * the side it arrives at.
 */
private const val ROOM_TO_TURN = 45.0

private const val SAMPLES_ALONG_CURVE = 32

/**
 * How far across an arrow that runs beside another may go for the distance it runs out from its source. Further
 * across than this, the two curves slant so much between the layers that they run into each other.
 */
private const val STEEPEST_BESIDE_ANOTHER = 0.5

/** The least distance out from its source that the curve of an arrow beside another covers when it starts further out. */
private const val SHORTEST_CURVE_BESIDE_ANOTHER = 20.0

/**
 * How far from the middle of the sides they join each of two arrows between the same two layers, one each way,
 * attaches. Enough for the two arrowheads to clear each other.
 */
const val RECIPROCAL_ARROW_SHIFT = 22.0

/**
 * The widest that something centered on one of two such arrows can be without covering the other, for arrows of the
 * usual thickness.
 */
const val WIDEST_ON_RECIPROCAL_ARROW = 64.0

/**
 * Builder function for a bezier arrow
 */
fun bezierArrow(init: BezierArrowTemplate.() -> Unit) = BezierArrow(BezierArrowTemplate().apply(init))