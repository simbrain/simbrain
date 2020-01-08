package org.simbrain.util

import java.awt.geom.Point2D

fun point(x: Double, y: Double) = Point2D.Double(x, y)
fun point(x: Int, y: Int) = Point2D.Double(x.toDouble(), y.toDouble())

operator fun Point2D.plus(other: Point2D) = Point2D.Double(this.x + other.x, this.y + other.y)
operator fun Point2D.minus(other: Point2D) = Point2D.Double(this.x - other.x, this.y - other.y)