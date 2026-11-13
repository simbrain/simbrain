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

        // 1. for each source and target, find a side of the outline to let the arrow connect
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
                }.also {
                    if (it == null) {
                        shaft = null
                        isDetour = false
                        hasOwnLane = false
                        updateEvent(null)
                    }
                } ?: return null

        // 2. compute the curve
        val shift = shiftFor(sourceSide, targetSide, requestedShift)
        // Two arrows that should run side by side cannot where one of the sides they join is too short for both
        val aroundTheCorner = if (shift < requestedShift - 0.5) {
            aroundTheCorner(sourceSide, targetSide, sourceOutlines, targetOutlines)
        } else {
            null
        }
        val directCurve = aroundTheCorner?.first ?: cubicBezier(
                sourceSide.tailOffset(shift),
                sourceSide.tailOffset(shift) + deltaVector.abs * sourceSide.unitNormal * 0.5,
                targetSide.headOffset(shift) + deltaVector.abs * targetSide.unitNormal * 0.5,
                targetSide.headOffset(shift)
        )
        val inTheWay = blockers.filter { directCurve.passesThrough(it) }
        val detour = if (inTheWay.isEmpty()) null else detourAround(inTheWay, sourceOutlines, targetOutlines, requestedShift)
        val curveModel = detour?.first ?: directCurve
        val headSide = detour?.second ?: aroundTheCorner?.second ?: targetSide
        isDetour = detour != null
        hasOwnLane = detour != null || aroundTheCorner != null
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