package org.simbrain.util

import java.awt.geom.CubicCurve2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs

/**
 * A smooth directed curve made of chained cubic bezier segments passing through a list of knots.
 *
 * Generalizes the single-cubic connector of [org.simbrain.util.widgets.BezierArrow] to routes with
 * interior waypoints, and provides a global parameterization for placing decorations along the curve.
 */
class BezierRoute(val segments: List<CubicCurve2D>) {

    init {
        require(segments.isNotEmpty()) { "A route needs at least one segment" }
    }

    val path: Path2D.Double = Path2D.Double().apply {
        moveTo(segments.first().x1, segments.first().y1)
        segments.forEach { curveTo(it.ctrlX1, it.ctrlY1, it.ctrlX2, it.ctrlY2, it.x2, it.y2) }
    }

    private val weights = segments.map { (it.p1 distanceTo it.p2).coerceAtLeast(1e-9) }
    private val totalWeight = weights.sum()

    private fun locate(t: Double): Pair<CubicCurve2D, Double> {
        val target = t.coerceIn(0.0, 1.0) * totalWeight
        var acc = 0.0
        segments.forEachIndexed { i, segment ->
            if (target <= acc + weights[i] || i == segments.lastIndex) {
                return segment to ((target - acc) / weights[i]).coerceIn(0.0, 1.0)
            }
            acc += weights[i]
        }
        error("unreachable")
    }

    /**
     * The point at global parameter [t] in 0..1, weighted across segments by chord length.
     */
    fun pointAt(t: Double): Point2D {
        val (segment, local) = locate(t)
        return segment.p(local)
    }

    /**
     * Unit tangent at global parameter [t], pointing in the direction of travel.
     */
    fun tangentAt(t: Double): Point2D {
        val (segment, local) = locate(t)
        val derivative = segment.derivative(local)
        return (if (derivative.magnitudeSq < 1e-12) segment.p2 - segment.p1 else derivative).norm
    }

    val start: Point2D get() = segments.first().p1
    val end: Point2D get() = segments.last().p2
    val endTangent: Point2D get() = tangentAt(1.0)
}

/**
 * First derivative of the cubic at parameter [t].
 */
fun CubicCurve2D.derivative(t: Double): Point2D {
    val u = 1 - t
    return (ctrlP1 - p1) * (3 * u * u) + (ctrlP2 - ctrlP1) * (6 * u * t) + (p2 - ctrlP2) * (3 * t * t)
}

/**
 * Build a [BezierRoute] through the given knots. The curve leaves the first knot along
 * [startNormal] and arrives at the last knot against [endNormal] (both outward unit normals of the
 * attached rectangle sides), reproducing [org.simbrain.util.widgets.BezierArrow]'s perpendicular
 * control extrusion at the endpoints; interior knots get Catmull-Rom tangents.
 */
fun routeThrough(knots: List<Point2D>, startNormal: Point2D, endNormal: Point2D): BezierRoute {
    require(knots.size >= 2) { "A route needs at least two knots" }
    val tangents = knots.indices.map { i ->
        when (i) {
            0 -> startNormal * (abs((knots[1] - knots[0]) dot startNormal) * 1.5).coerceAtLeast(20.0)
            knots.lastIndex -> -endNormal * (abs((knots[i] - knots[i - 1]) dot endNormal) * 1.5).coerceAtLeast(20.0)
            else -> (knots[i + 1] - knots[i - 1]) * 0.5
        }
    }
    val segments = (0 until knots.lastIndex).map { i ->
        cubicBezier(
            knots[i],
            knots[i] + tangents[i] / 3.0,
            knots[i + 1] - tangents[i + 1] / 3.0,
            knots[i + 1]
        )
    }
    return BezierRoute(segments)
}

/**
 * Route a curve from the boundary of [sourceBounds] to the boundary of [targetBounds] through the
 * given [waypoints]. Attach sides are chosen to face the nearest guide point (the first/last
 * waypoint, or the other rectangle's center when there are none), so the curve exits and enters
 * perpendicular to the tile borders.
 */
fun bezierRoute(
    sourceBounds: Rectangle2D,
    targetBounds: Rectangle2D,
    waypoints: List<Point2D> = emptyList(),
    tailPadding: Double = 0.0,
    headPadding: Double = 0.0
): BezierRoute {
    val sourceGuide = waypoints.firstOrNull() ?: targetBounds.center
    val targetGuide = waypoints.lastOrNull() ?: sourceBounds.center
    val sourceSide = sourceBounds.outlines.toList().maxBy { it.unitNormal dot (sourceGuide - it.midPoint).norm }
    val targetSide = targetBounds.outlines.toList().maxBy { it.unitNormal dot (targetGuide - it.midPoint).norm }
    val tail = sourceSide.midPoint + sourceSide.unitNormal * tailPadding
    val head = targetSide.midPoint + targetSide.unitNormal * headPadding
    return routeThrough(listOf(tail) + waypoints + listOf(head), sourceSide.unitNormal, targetSide.unitNormal)
}
