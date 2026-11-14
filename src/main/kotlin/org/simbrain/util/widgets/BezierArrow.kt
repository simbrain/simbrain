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

        // 3. transform the tip onto the head location and union it with the stroked curve
        val tipTransform = AffineTransform().apply {
            translate(curveModel.x2, curveModel.y2)
            rotate(headSide.normalTheta)
        }
        val arrowShape = Area(BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(curveModel)).apply {
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

    /** A way for the arrow to run between two outlines. */
    private class Plan(val curve: CubicCurve2D, val headSide: Line2D, val route: ArrowRoute, val hasOwnLane: Boolean)

    /**
     * The way the arrow runs when no sides are asked for: between the nearest pair of sides that face each other,
     * or, for one of two arrows between the same layers, the pair that leaves its source most squarely.
     */
    private fun usualPlan(sourceOutlines: RectangleSides, targetOutlines: RectangleSides, bidirectional: Boolean): Plan? {
        val requestedShift = _lateralShift()
        val (deltaVector, sourceSide, targetSide) = (sourceOutlines.toList() cartesianProduct targetOutlines.toList())
                .map { (source, target) ->
                    val shift = shiftFor(source, target, requestedShift)
                    Triple(target.headOffset(shift) - source.tailOffset(shift), source, target)
                }
                .filter { (line, source, target) ->
                    // make sure the curve does not bend backward
                    line.norm dot source.unitNormal > 0.3 && line.norm dot target.unitNormal < -0.3
                }
                .let {
                    if (bidirectional) {
                        it.maxByOrNull { (line, source, _) -> line dot source.normal }
                    } else {
                        it.minByOrNull { (_, source, target) -> source.midPoint distanceSqTo target.midPoint }
                    }
                } ?: return null
        val sides = ArrowRoute(sourceOutlines.toList().indexOf(sourceSide), targetOutlines.toList().indexOf(targetSide))
        val shift = shiftFor(sourceSide, targetSide, requestedShift)
        // Two arrows that should run side by side cannot where one of the sides they join is too short for both
        if (shift < requestedShift - 0.5) {
            aroundTheCorner(sourceSide, targetSide, sourceOutlines, targetOutlines)?.let { (curve, arrivalSide) ->
                return Plan(curve, arrivalSide, sides, hasOwnLane = true)
            }
        }
        val curve = cubicBezier(
                sourceSide.tailOffset(shift),
                sourceSide.tailOffset(shift) + deltaVector.abs * sourceSide.unitNormal * 0.5,
                targetSide.headOffset(shift) + deltaVector.abs * targetSide.unitNormal * 0.5,
                targetSide.headOffset(shift)
        )
        return Plan(curve, targetSide, sides, hasOwnLane = false)
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
        val shift = shiftFor(sourceSide, targetSide, _lateralShift())
        val delta = targetSide.headOffset(shift) - sourceSide.tailOffset(shift)
        val bendsBackward = !(delta.norm dot sourceSide.unitNormal > 0.3 && delta.norm dot targetSide.unitNormal < -0.3)
        if (bendsBackward) return null
        val curve = cubicBezier(
                sourceSide.tailOffset(shift),
                sourceSide.tailOffset(shift) + delta.abs * sourceSide.unitNormal * 0.5,
                targetSide.headOffset(shift) + delta.abs * targetSide.unitNormal * 0.5,
                targetSide.headOffset(shift)
        )
        return Plan(curve, targetSide, route, hasOwnLane = false)
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
        return plan?.let { RoutedShaft(it.curve, it.hasOwnLane) }
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
 */
class RoutedShaft(val curve: CubicCurve2D, val hasOwnLane: Boolean)

/** Space left between an arrow that goes around something and what it goes around, for what is drawn on the arrow. */
private const val DETOUR_CLEARANCE = 80.0

/** The least an arrow that goes around something bows out. */
private const val MINIMUM_BOW = 60.0

/** Room an arrow that attaches around the corner from a short side has to turn in to the side it attaches to. */
private const val ROOM_TO_TURN = 45.0

private const val SAMPLES_ALONG_CURVE = 32

/**
 * How far from the middle of the sides they join each of two arrows between the same two layers, one each way,
 * attaches. Enough for the two arrowheads to clear each other.
 */
const val RECIPROCAL_ARROW_SHIFT = 22.0

/**
 * Builder function for a bezier arrow
 */
fun bezierArrow(init: BezierArrowTemplate.() -> Unit) = BezierArrow(BezierArrowTemplate().apply(init))