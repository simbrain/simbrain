package org.simbrain.util

/**
 * Kotlin utility classes for 2d geometry.
 */
import org.piccolo2d.util.PDimension
import java.awt.Point
import java.awt.Polygon
import java.awt.geom.CubicCurve2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.lang.Double.isNaN
import kotlin.math.*

fun Int.toRadian() = Math.toRadians(this.toDouble())
fun Double.toRadian() = Math.toRadians(this)

fun Int.toDegrees() = Math.toDegrees(this.toDouble())
fun Double.toDegrees() = Math.toDegrees(this)


data class IntPoint(val x: Int, val y: Int) {
    fun toPoint2D() = point(x, y)
}

val Point2D.int get() = IntPoint(x.toInt(), y.toInt())

// Points / Vectors
fun point(x: Double, y: Double): Point2D = Point2D.Double(x, y)

fun point(x: Int, y: Int): Point = Point(x, y)

infix fun Point2D.distanceTo(other: Point2D) = this.distance(other)
infix fun Point2D.distanceSqTo(other: Point2D) = this.distanceSq(other)

/**
 * Calculates distance considering wrap-around (torus topology).
 * Uses the shortest path, which may cross the world boundary.
 */
fun Point2D.wrapAroundDistanceTo(other: Point2D, worldWidth: Double, worldHeight: Double): Double {
    val dxDirect = abs(this.x - other.x)
    val dyDirect = abs(this.y - other.y)
    val dx = min(dxDirect, worldWidth - dxDirect)
    val dy = min(dyDirect, worldHeight - dyDirect)
    return sqrt(dx * dx + dy * dy)
}

/**
 * Shortest vector from this point to [other] on a torus of the given size.
 * The returned vector's direction is the shortest path; magnitude equals [wrapAroundDistanceTo].
 */
fun Point2D.wrapAroundVectorTo(other: Point2D, worldWidth: Double, worldHeight: Double): Point2D {
    var dx = other.x - this.x
    var dy = other.y - this.y
    if (dx > worldWidth / 2) dx -= worldWidth else if (dx < -worldWidth / 2) dx += worldWidth
    if (dy > worldHeight / 2) dy -= worldHeight else if (dy < -worldHeight / 2) dy += worldHeight
    return point(dx, dy)
}

/**
 * Distance from ([startX], [startY]) along unit direction ([dirX], [dirY]) to the first
 * intersection with the AABB at top-left ([boxX], [boxY]) and size ([boxW] x [boxH]).
 * Returns [maxDist] if the box is not hit within that range, or 0.0 if the ray origin is
 * already inside the box. Uses the standard slab method.
 */
fun rayVsAabb(
    startX: Double, startY: Double, dirX: Double, dirY: Double,
    boxX: Double, boxY: Double, boxW: Double, boxH: Double,
    maxDist: Double
): Double {
    var tmin = Double.NEGATIVE_INFINITY
    var tmax = Double.POSITIVE_INFINITY
    if (abs(dirX) < 1e-12) {
        if (startX < boxX || startX > boxX + boxW) return maxDist
    } else {
        val tx1 = (boxX - startX) / dirX
        val tx2 = (boxX + boxW - startX) / dirX
        tmin = max(tmin, min(tx1, tx2))
        tmax = min(tmax, max(tx1, tx2))
    }
    if (abs(dirY) < 1e-12) {
        if (startY < boxY || startY > boxY + boxH) return maxDist
    } else {
        val ty1 = (boxY - startY) / dirY
        val ty2 = (boxY + boxH - startY) / dirY
        tmin = max(tmin, min(ty1, ty2))
        tmax = min(tmax, max(ty1, ty2))
    }
    if (tmax < 0.0 || tmin > tmax) return maxDist
    if (tmin < 0.0) return 0.0
    return min(tmin, maxDist)
}

operator fun Point2D.unaryMinus() = point(-x, -y)

operator fun Point2D.plus(vector: Point2D) = point(this.x + vector.x, this.y + vector.y)
operator fun Point2D.minus(other: Point2D) = point(this.x - other.x, this.y - other.y)
operator fun Point2D.plus(vector: PDimension) = point(this.x + vector.width, this.y + vector.height)
operator fun Point2D.minus(vector: PDimension) = point(this.x - vector.width, this.y - vector.height)

operator fun Point2D.times(vector: Point2D) = point(x * vector.x, y * vector.y)

operator fun Point2D.times(scalar: Double) = point(x * scalar, y * scalar)
operator fun Point2D.div(scalar: Double) = if (scalar == 0.0) {
    point(0, 0)
} else {
    point(x / scalar, y / scalar)
}

operator fun Point2D.times(scalar: Int) = this * scalar.toDouble()
operator fun Point2D.div(scalar: Int) = this / scalar.toDouble()

infix fun Point2D.dot(other: Point2D) = this.x * other.x + this.y * other.y

fun Point2D.rotate(radian: Double)
        = point(cos(radian) * x - sin(radian) * y, sin(radian) * x + cos(radian) * y)

fun Point2D.rotate(radian: Double, pivot: Point2D = point(0, 0))
    = ((this - pivot).rotate(radian)) + pivot

val Point2D.norm
    get() = this / magnitude

val Point2D.magnitudeSq
    get() = x * x + y * y

val Point2D.magnitude
    get() = sqrt(magnitudeSq)

val Point2D.abs
    get() = point(x.absoluteValue, y.absoluteValue)

operator fun Point2D.component1() = x
operator fun Point2D.component2() = y

operator fun Point.component1() = x
operator fun Point.component2() = y

fun Point2D.copy() = point(x, y)

// Lines
fun line(p1: Point2D, p2: Point2D) = Line2D.Double(p1, p2)

val Line2D.vector: Point2D
    get() {
        val vector = p2 - p1
        return point(vector.x, vector.y)
    }

val Line2D.normal : Point2D
    get() {
        val vector = p2 - p1
        return point(-vector.y, vector.x)
    }

val Line2D.unitNormal
    get() = normal.norm

val Line2D.normalTheta
    get() = with(unitNormal) {
        if (x == 0.0) {
            acos(y)
        } else {
            -asin(x)
        }
    }

val Line2D.midPoint
    get() = p(0.5)

/**
 * Returns the point at t% of the line
 */
fun Line2D.p(t: Double) = point(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)

fun Line2D.scale(t: Double) = p(t)

// Rectangles
fun rectangle(p1: Point2D, p2: Point2D): Rectangle2D {
    val x = min(p1.x, p2.x)
    val y = min(p1.y, p2.y)
    val w = abs(p2.x - p1.x)
    val h = abs(p2.y - p1.y)
    return Rectangle2D.Double(x, y, w, h)
}

val Rectangle2D.vertices get() = RectangleVertices(
        point(x, y),
        point(x + width, y),
        point(x, y + height),
        point(x + width, y + height)
)

val Rectangle2D.outlines get() = vertices.sides

/**
 * Build attachment outlines using [topBounds] for the top edge and [sideBounds] for the lateral and bottom edges.
 *
 * This is useful for nodes whose interaction box extends above the main body: vertical arrows can hit the visible
 * top border, while left/right arrows still attach to the tighter body bounds.
 */
fun directionalOutlines(topBounds: Rectangle2D, sideBounds: Rectangle2D): RectangleSides = RectangleSides(
    line(
        point(topBounds.x + topBounds.width, topBounds.y),
        point(topBounds.x, topBounds.y)
    ),
    line(
        point(sideBounds.x + sideBounds.width, sideBounds.y + sideBounds.height),
        point(sideBounds.x + sideBounds.width, sideBounds.y)
    ),
    line(
        point(sideBounds.x, sideBounds.y + sideBounds.height),
        point(sideBounds.x + sideBounds.width, sideBounds.y + sideBounds.height)
    ),
    line(
        point(sideBounds.x, sideBounds.y),
        point(sideBounds.x, sideBounds.y + sideBounds.height)
    )
)

fun Rectangle2D.expandBy(vector: Point2D): Rectangle2D {
    val (width, height) = width + vector.x to height + vector.y
    val x = if (vector.x < 0) x - vector.x else x
    val y = if (vector.y < 0) y - vector.y else y
    return Rectangle2D.Double(x, y, width, height)
}

data class RectangleVertices(
        val topLeft: Point2D,
        val topRight: Point2D,
        val bottomLeft: Point2D,
        val bottomRight: Point2D
)

fun RectangleVertices.toList() = listOf(topLeft, topRight, bottomRight, bottomLeft)

data class RectangleSides(val top: Line2D, val right: Line2D, val bottom: Line2D, val left: Line2D) {
    fun toList() = listOf(top, right, bottom, left)
}

val RectangleVertices.sides get() = RectangleSides(
        line(topRight, topLeft),
        line(bottomRight, topRight),
        line(bottomLeft, bottomRight),
        line(topLeft, bottomLeft)
)

fun Rectangle2D.addPadding(px: Double): Rectangle2D {
    return rectangle(
        vertices.topLeft - point(px, px),
        vertices.bottomRight + point(px, px)
    )
}

// Polygons
fun polygon(vararg points: Point2D) = points.toList().toPolygon()
fun polygon(points: Collection<Point2D>) = points.toPolygon()
fun Collection<Point2D>.toPolygon() = Polygon(
        map { it.x.roundToInt() }.toIntArray(),
        map { it.y.roundToInt() }.toIntArray(),
        size
)
fun Collection<Point2D>.rotate(radian: Double, pivot: Point2D) = map { it.rotate(radian, pivot) }
fun Polygon.rotate(radian: Double, pivot: Point2D)
    = (xpoints zip ypoints).map { point(it.first, it.second) }.rotate(radian, pivot).toPolygon()


fun Polygon.translate(vector: Point2D) = apply {
    translate(vector.x.roundToInt(), vector.y.roundToInt())
}

// Curves
fun cubicBezier(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D)
    = CubicCurve2D.Double(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

val CubicCurve2D.midpoint: Point2D
    get() = p(0.5)

fun CubicCurve2D.p(t: Double): Point2D {
    val m11 = line(p1, ctrlP1).p(t)
    val m12 = line(ctrlP1, ctrlP2).p(t)
    val m13 = line(ctrlP2, p2).p(t)

    val m21 = line(m11, m12).p(t)
    val m22 = line(m12, m13).p(t)

    return line(m21, m22).p(t)
}

operator fun Rectangle2D.component1() = x
operator fun Rectangle2D.component2() = y
operator fun Rectangle2D.component3() = width
operator fun Rectangle2D.component4() = height

val Rectangle2D.center get() = point(centerX, centerY)
val Rectangle2D.topLeft get() = point(x, y)
val Rectangle2D.centerLeft get() = point(x, y + height / 2)

fun Rectangle2D.centerOn(point: Point2D) {
    val (px, py) = point
    setRect(px - width / 2, py - height / 2, width, height)
}

fun Rectangle2D.setTopLeftLocation(point: Point2D) {
    val (x, y) = point
    setRect(x, y, width, height)
}

fun Rectangle2D.setTopLeftLocation(x: Double, y: Double) {
    setRect(x, y, width, height)
}

fun Rectangle2D.setSize(width: Double, height: Double) {
    setRect(x, y, width, height)
}

operator fun Rectangle2D.plus(point: Point2D): Rectangle2D.Double {
    return Rectangle2D.Double(x + point.x, y + point.y, width, height)
}

fun Point2D.withVector(vector: Point2D): Line2D = Line2D.Double(x, y, x + vector.x, y + vector.y)
fun Point2D.withVector(u: Double, v: Double): Line2D = Line2D.Double(x, y, x + u, y + v)
fun Point2D.withVector(u: Int, v: Int): Line2D = Line2D.Double(x, y, x + u, y + v)

infix fun Point2D.cross(other: Point2D) = this.x * other.y - other.x * this.y

fun Point2D.format(digits: Int) = "(${x.format(digits)}, ${y.format(digits)})"

sealed interface Intersection {
    data class Time(val time: Double): Intersection
    data class Point(val point: Point2D): Intersection
    object Overlap : Intersection
    object Empty: Intersection
}

fun Line2D.intersectionTime(other: Line2D, inclusive: Boolean = true): Intersection {
    if (!this.intersectsLine(other)) {
        return Intersection.Empty
    }
    val result = ((other.p1 - this.p1) cross other.vector) / (this.vector cross other.vector)
    return when {
        isNaN(result) -> Intersection.Overlap
        else -> if (inclusive || (result != 0.0 && result != 1.0)) {
            Intersection.Time(result)
        } else {
            Intersection.Empty
        }
    }
}

fun Line2D.intersectionPoint(other: Line2D): Intersection {
    return when (val t = intersectionTime(other)) {
        is Intersection.Time -> Intersection.Point(scale(t.time))
        else -> t
    }
}

typealias Degree = Double
typealias Radian = Double

fun computeIntersection(rect1: Rectangle2D, rect2: Rectangle2D): Rectangle2D? {
    val x = max(rect1.x, rect2.x)
    val y = max(rect1.y, rect2.y)
    val width = min(rect1.x + rect1.width, rect2.x + rect2.width) - x
    val height = min(rect1.y + rect1.height, rect2.y + rect2.height) - y
    return if (width > 0 && height > 0) {
        Rectangle2D.Double(x, y, width, height)
    } else {
        null
    }
}

infix fun Rectangle2D.intersects(other: Rectangle2D) = computeIntersection(this, other)

/**
 * Given a source and target rectangle return a function that takes any point in the source rectangle and returns the
 * appropriate transformed point in the target rectangle
 */
fun getTransformationFunction(source: Rectangle2D, target: Rectangle2D): (Point2D) -> Point2D {
    val (sx, sy, sw, sh) = source
    val (tx, ty, tw, th) = target
    val scaleX = tw / sw
    val scaleY = th / sh
    val offsetX = tx - sx * scaleX
    val offsetY = ty - sy * scaleY
    return { point(it.x * scaleX + offsetX, it.y * scaleY + offsetY) }
}

/**
 * For each pixel in the rectangle, apply the given action
 */
fun Rectangle2D.forEachPixel(action: (x: Int, y: Int) -> Unit) {
    for (x in this.x.toInt() until this.x.toInt() + this.width.toInt()) {
        for (y in this.y.toInt() until this.y.toInt() + this.height.toInt()) {
            action(x, y)
        }
    }
}

fun Rectangle2D.uv(uvPoint: Point2D): Point2D {
    val (u, v) = uvPoint
    return point(width * u, height * v) + point(x, y)
}

val sin60deg = sin(Math.toRadians(60.0))
