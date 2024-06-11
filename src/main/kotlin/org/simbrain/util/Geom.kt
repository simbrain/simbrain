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

val sin60deg = sin(Math.toRadians(60.0))