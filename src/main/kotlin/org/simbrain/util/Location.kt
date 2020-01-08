package org.simbrain.util

import java.awt.geom.Point2D

enum class Anchor {
    TopLeft, Center
}

data class Location(
        val x: Double,
        val y: Double,
        val width: Double = 0.0,
        val height: Double = 0.0,
        val anchor: Anchor = Anchor.Center
) {

    val centerLocation by lazy {
        when (anchor) {
            Anchor.TopLeft -> point(x + width / 2, y + height / 2)
            Anchor.Center -> point(x, y)
        }
    }

    val topLeftLocation by lazy {
        when (anchor) {
            Anchor.TopLeft -> point(x, y)
            Anchor.Center -> point(x - width / 2, y - height / 2)
        }
    }

    val anchorLocation = point(x, y)

    fun copy(point: Point2D) = copy(x = point.x, y = point.y)

}