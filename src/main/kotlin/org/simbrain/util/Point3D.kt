package org.simbrain.util

import java.awt.geom.Point2D

data class Point3D(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0) {

    val point2D = Point2D.Double(x, y)

    fun setCopy2D(point2D: Point2D) = copy(x = point2D.x, y = point2D.y)

    fun setCopyX(x: Double) = copy(x = x)
    fun setCopyY(y: Double) = copy(y = y)
    fun setCopyZ(z: Double) = copy(z = z)

    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
}