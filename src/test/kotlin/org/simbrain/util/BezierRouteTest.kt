package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.geom.Rectangle2D

class BezierRouteTest {

    @Test
    fun `route passes through every knot`() {
        val knots = listOf(point(0.0, 0.0), point(100.0, 50.0), point(200.0, 0.0), point(300.0, 80.0))
        val route = routeThrough(knots, startNormal = point(1.0, 0.0), endNormal = point(-1.0, 0.0))
        assertEquals(3, route.segments.size)
        knots.forEachIndexed { i, knot ->
            val segmentPoint = when (i) {
                0 -> route.segments.first().p1
                else -> route.segments[i - 1].p2
            }
            assertEquals(knot.x, segmentPoint.x, 1e-9)
            assertEquals(knot.y, segmentPoint.y, 1e-9)
        }
        assertEquals(0.0, route.pointAt(0.0).x, 1e-9)
        assertEquals(300.0, route.pointAt(1.0).x, 1e-9)
    }

    @Test
    fun `single segment matches bezier arrow control extrusion`() {
        val tail = point(0.0, 0.0)
        val head = point(100.0, 40.0)
        val route = routeThrough(listOf(tail, head), startNormal = point(1.0, 0.0), endNormal = point(-1.0, 0.0))
        val segment = route.segments.single()
        assertEquals(tail.x + 50.0, segment.ctrlP1.x, 1e-9)
        assertEquals(tail.y, segment.ctrlP1.y, 1e-9)
        assertEquals(head.x - 50.0, segment.ctrlP2.x, 1e-9)
        assertEquals(head.y, segment.ctrlP2.y, 1e-9)
    }

    @Test
    fun `end tangent points into the target`() {
        val route = routeThrough(
            listOf(point(0.0, 0.0), point(200.0, 100.0)),
            startNormal = point(1.0, 0.0),
            endNormal = point(-1.0, 0.0)
        )
        val tangent = route.endTangent
        assertTrue(tangent.x > 0.99) { "expected tangent along +x into the target, got $tangent" }
    }

    @Test
    fun `bezier route attaches to the facing sides of the rectangles`() {
        val source = Rectangle2D.Double(0.0, 0.0, 40.0, 40.0)
        val target = Rectangle2D.Double(200.0, 0.0, 40.0, 40.0)
        val route = bezierRoute(source, target)
        assertEquals(40.0, route.start.x, 1e-9)
        assertEquals(20.0, route.start.y, 1e-9)
        assertEquals(200.0, route.end.x, 1e-9)
        assertEquals(20.0, route.end.y, 1e-9)
    }

    @Test
    fun `waypoints steer side selection`() {
        val source = Rectangle2D.Double(0.0, 0.0, 40.0, 40.0)
        val target = Rectangle2D.Double(0.0, 300.0, 40.0, 40.0)
        val waypoint = point(150.0, 60.0)
        val route = bezierRoute(source, target, waypoints = listOf(waypoint))
        assertEquals(40.0, route.start.x, 1e-9) { "curve should leave the source's right side toward the waypoint" }
        assertEquals(300.0, route.end.y, 1e-9) { "curve should enter the target's top side from the waypoint" }
        val mid = route.pointAt(0.5)
        assertTrue(mid.x > 60.0) { "route should bow out toward the waypoint, got $mid" }
    }

    @Test
    fun `pointAt weights segments by chord length`() {
        val route = routeThrough(
            listOf(point(0.0, 0.0), point(300.0, 0.0), point(400.0, 0.0)),
            startNormal = point(1.0, 0.0),
            endNormal = point(-1.0, 0.0)
        )
        val threeQuarters = route.pointAt(0.75)
        assertEquals(300.0, threeQuarters.x, 1.0)
    }
}
