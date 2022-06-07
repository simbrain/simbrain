package org.simbrain.world

import org.junit.jupiter.api.Test
import org.simbrain.util.*
import org.simbrain.world.odorworld.OdorWorld
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import kotlin.math.min

class OdorWorldMovementTest {

    @Test
    fun asdf() {

        val world = OdorWorld()

        val mouse = world.addAgent()
        mouse.location = point(100, 100)
        val cheese = world.addEntity()
        cheese.location = point(140.5, 101.0)

        with(mouse) {

            val box = Rectangle2D.Double(x, y, width, height)

            val dx = 1.0
            val dy = 0.0

            val bounds = world.entityList
                .filter { it !== this }
                .map { with(it) { Rectangle2D.Double(x, y, width, height) } }

            val boundLines = bounds.flatMap { it.outlines.toList() }
            val boundVertices = bounds.flatMap { it.vertices.toList() }

            fun List<Pair<Line2D, Line2D>>.maximumTime() = map { (a, b) -> a.intersectionTime(b) }
                .filterIsInstance<Intersection.Time>()
                .map { it.time }
                .minByOrNull { it } ?: 1.0

            val moveInX = Rectangle2D.Double(x, y, width, height)
                .vertices
                .toList()
                .map { point(it.x, it.y).withVector(point(dx, 0.0)) }
            val xt = (boundLines cartesianProduct moveInX).maximumTime()

            val boundsWithDx = boundVertices.map { it.withVector(point(-dx, 0.0)) }
            val xt2 = (box.outlines.toList() cartesianProduct boundsWithDx).maximumTime()

            val landingSpotX = location + point(dx * min(xt, xt2), 0.0)
            val (xdx, ydx) = landingSpotX

            val moveInY = Rectangle2D.Double(xdx, ydx, width, height)
                .vertices
                .toList()
                .map { point(it.x, it.y).withVector(point(0.0, dy)) }
            val yt = (boundLines cartesianProduct moveInY).maximumTime()

            val landingSpotXY = landingSpotX + point(0.0, dy * yt)
        }
    }


}