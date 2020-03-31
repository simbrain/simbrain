package org.simbrain.util

import java.awt.Point
import java.awt.Polygon
import java.awt.geom.CubicCurve2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.*

fun Int.toRadian() = Math.toRadians(this.toDouble())
fun Double.toRadian() = Math.toRadians(this)

// Points / Vectors
fun point(x: Double, y: Double): Point2D = Point2D.Double(x, y)

fun point(x: Int, y: Int): Point2D = Point2D.Double(x.toDouble(), y.toDouble())

infix fun Point2D.distanceTo(other: Point2D) = this.distance(other)
infix fun Point2D.distanceSqTo(other: Point2D) = this.distanceSq(other)

operator fun Point2D.unaryMinus() = point(-x, -y)

operator fun Point2D.plus(vector: Point2D) = point(this.x + vector.x, this.y + vector.y)
operator fun Point2D.minus(other: Point2D) = point(this.x - other.x, this.y - other.y)

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

// Lines
fun line(p1: Point2D, p2: Point2D) = Line2D.Double(p1, p2)

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
            acos(-y)
        } else {
            asin(x)
        }
    }

val Line2D.midPoint
    get() = p(0.5)

fun Line2D.p(t: Double) = point(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)


// Rectangles
fun rectangle(p1: Point2D, p2: Point2D) = Rectangle2D.Double(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y)

val Rectangle2D.vertices get() = RectangleVertices(
        point(x, y),
        point(x + width, y),
        point(x, y + height),
        point(x + width, y + height)
)

val Rectangle2D.outlines get() = vertices.outlines

data class RectangleVertices(
        val topLeft: Point2D,
        val topRight: Point2D,
        val bottomLeft: Point2D,
        val bottomRight: Point2D
)

data class RectangleOutlines(val top: Line2D, val right: Line2D, val bottom: Line2D, val left: Line2D) {
    fun toList() = listOf(top, right, bottom, left)
}

val RectangleVertices.outlines get() = RectangleOutlines(
        line(topRight, topLeft),
        line(bottomRight, topRight),
        line(bottomLeft, bottomRight),
        line(topLeft, bottomLeft)
)

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