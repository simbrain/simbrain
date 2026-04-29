package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.geom.Line2D

class GeomTest {

    // Wrap-around distance tests

    @Test
    fun `wrap around distance across x boundary should be shorter than direct distance`() {
        // In a 300x300 world, point at x=10 and x=290 are 20 units apart via wrap-around
        val p1 = point(10.0, 150.0)
        val p2 = point(290.0, 150.0)
        val distance = p1.wrapAroundDistanceTo(p2, 300.0, 300.0)
        assertEquals(20.0, distance, 0.001)
    }

    @Test
    fun `wrap around distance across y boundary should be shorter than direct distance`() {
        // In a 300x300 world, point at y=10 and y=290 are 20 units apart via wrap-around
        val p1 = point(150.0, 10.0)
        val p2 = point(150.0, 290.0)
        val distance = p1.wrapAroundDistanceTo(p2, 300.0, 300.0)
        assertEquals(20.0, distance, 0.001)
    }

    @Test
    fun `wrap around distance when direct is shorter should use direct distance`() {
        // Points that are closer directly than via wrap-around
        val p1 = point(100.0, 100.0)
        val p2 = point(120.0, 100.0)
        val distance = p1.wrapAroundDistanceTo(p2, 300.0, 300.0)
        assertEquals(20.0, distance, 0.001)
    }

    @Test
    fun `wrap around distance across both boundaries`() {
        // Corner to corner wrap: (10,10) to (290,290) in 300x300 world
        // dx = min(280, 20) = 20, dy = min(280, 20) = 20
        // distance = sqrt(20^2 + 20^2) = sqrt(800) ≈ 28.28
        val p1 = point(10.0, 10.0)
        val p2 = point(290.0, 290.0)
        val distance = p1.wrapAroundDistanceTo(p2, 300.0, 300.0)
        assertEquals(28.284, distance, 0.001)
    }

    @Test
    fun `wrap around distance for same point should be zero`() {
        val p1 = point(150.0, 150.0)
        val distance = p1.wrapAroundDistanceTo(p1, 300.0, 300.0)
        assertEquals(0.0, distance, 0.001)
    }

    // Wrap-around vector tests

    @Test
    fun `wrap around vector when direct is shorter returns direct vector`() {
        val p1 = point(100.0, 100.0)
        val p2 = point(120.0, 100.0)
        val v = p1.wrapAroundVectorTo(p2, 300.0, 300.0)
        assertEquals(20.0, v.x, 0.001)
        assertEquals(0.0, v.y, 0.001)
    }

    @Test
    fun `wrap around vector across right edge points the short way`() {
        // From x=10 to x=290 in 300 wide: direct +280 vs wrapped -20. Wrap wins.
        val p1 = point(10.0, 150.0)
        val p2 = point(290.0, 150.0)
        val v = p1.wrapAroundVectorTo(p2, 300.0, 300.0)
        assertEquals(-20.0, v.x, 0.001)
        assertEquals(0.0, v.y, 0.001)
    }

    @Test
    fun `wrap around vector across left edge points the short way`() {
        // From x=290 to x=10 in 300 wide: direct -280 vs wrapped +20. Wrap wins.
        val p1 = point(290.0, 150.0)
        val p2 = point(10.0, 150.0)
        val v = p1.wrapAroundVectorTo(p2, 300.0, 300.0)
        assertEquals(20.0, v.x, 0.001)
        assertEquals(0.0, v.y, 0.001)
    }

    @Test
    fun `wrap around vector across both axes`() {
        val p1 = point(10.0, 10.0)
        val p2 = point(290.0, 290.0)
        val v = p1.wrapAroundVectorTo(p2, 300.0, 300.0)
        assertEquals(-20.0, v.x, 0.001)
        assertEquals(-20.0, v.y, 0.001)
    }

    @Test
    fun `wrap around vector for same point is zero`() {
        val p = point(150.0, 150.0)
        val v = p.wrapAroundVectorTo(p, 300.0, 300.0)
        assertEquals(0.0, v.x, 0.001)
        assertEquals(0.0, v.y, 0.001)
    }

    @Test
    fun `wrap around vector magnitude matches wrap around distance`() {
        val p1 = point(10.0, 10.0)
        val p2 = point(290.0, 290.0)
        val v = p1.wrapAroundVectorTo(p2, 300.0, 300.0)
        val d = p1.wrapAroundDistanceTo(p2, 300.0, 300.0)
        assertEquals(d, v.magnitude, 0.001)
    }

    // rayVsAabb tests

    @Test
    fun `rayVsAabb hits axis-aligned box dead on`() {
        // Ray from (0, 50) heading right; box at x=100..150, y=0..100.
        val d = rayVsAabb(0.0, 50.0, 1.0, 0.0, 100.0, 0.0, 50.0, 100.0, 1000.0)
        assertEquals(100.0, d, 0.001)
    }

    @Test
    fun `rayVsAabb misses box returns maxDist`() {
        // Ray from (0, 200) heading right; box at x=100..150, y=0..100. Misses (above box).
        val d = rayVsAabb(0.0, 200.0, 1.0, 0.0, 100.0, 0.0, 50.0, 100.0, 1000.0)
        assertEquals(1000.0, d, 0.001)
    }

    @Test
    fun `rayVsAabb origin inside box returns zero`() {
        val d = rayVsAabb(110.0, 50.0, 1.0, 0.0, 100.0, 0.0, 50.0, 100.0, 1000.0)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun `rayVsAabb pointing away from box returns maxDist`() {
        // Ray at x=200 heading right (+x); box behind us at x=100..150.
        val d = rayVsAabb(200.0, 50.0, 1.0, 0.0, 100.0, 0.0, 50.0, 100.0, 1000.0)
        assertEquals(1000.0, d, 0.001)
    }

    @Test
    fun `rayVsAabb axis-aligned vertical ray hits box`() {
        // Ray from (50, 0) heading down (+y); box at y=100..150 spanning x=0..100.
        val d = rayVsAabb(50.0, 0.0, 0.0, 1.0, 0.0, 100.0, 100.0, 50.0, 1000.0)
        assertEquals(100.0, d, 0.001)
    }

    @Test
    fun `rayVsAabb axis-aligned ray that doesn't overlap slab returns maxDist`() {
        // Ray from (200, 0) heading down. Box at y=100..150, x=0..100. Vertical ray's x=200
        // is outside the box's x range, so it never enters.
        val d = rayVsAabb(200.0, 0.0, 0.0, 1.0, 0.0, 100.0, 100.0, 50.0, 1000.0)
        assertEquals(1000.0, d, 0.001)
    }

    @Test
    fun `rayVsAabb hit beyond maxDist clamps to maxDist`() {
        // Hit would be at distance 100 but maxDist is 50.
        val d = rayVsAabb(0.0, 50.0, 1.0, 0.0, 100.0, 0.0, 50.0, 100.0, 50.0)
        assertEquals(50.0, d, 0.001)
    }

    @Test
    fun `p(0, 1)v(0, 2) and p(1, 0)v(2, 0) should intersect at t = 0_5`() {
        val line1 = point(0, 1).withVector(2, 0)
        val line2 = point(1, 0).withVector(0, 2)
        val intersection = line1.intersectionTime(line2)
        assert(intersection is Intersection.Time)
        if (intersection is Intersection.Time){
            assertEquals(0.5, intersection.time, 0.01)
        }
    }

    @Test
    fun `p(0, 1)v(0, 2) and p(1, 0)v(2, 0) should intersect at p(1, 1)`() {
        val line1 = point(0, 1).withVector(2, 0)
        val line2 = point(1, 0).withVector(0, 2)
        val intersection = line1.intersectionPoint(line2)
        assert(intersection is Intersection.Point)
        if (intersection is Intersection.Point) {
            assertEquals(point(1, 1), intersection.point)
        }
    }

    @Test
    fun `line((-13 , -7), (59 , 22)) and line((2, -14), (-8, 4)) should intersect at around p(-3_92055 , -3_34300)`() {
        val line1 = Line2D.Double(-13.0, -7.0, 59.0, 22.0)
        val line2 = Line2D.Double(2.0, -14.0, -8.0, 4.0)
        val intersection = line1.intersectionPoint(line2)
        assert(intersection is Intersection.Point)
        if (intersection is Intersection.Point) {
            val (x, y) = intersection.point
            assertEquals(-3.92055, x, 0.0001)
            assertEquals(-3.34300, y, 0.0001)
        }
    }

    @Test
    fun `p(0, 0)v(0, 2) and p(1, 0)v(0, 2) should not intersect`() {
        val line1 = point(0, 0).withVector(0, 2)
        val line2 = point(1, 0).withVector(0, 2)
        val intersection = line1.intersectionTime(line2)
        assertEquals(Intersection.Empty, intersection)
    }

    @Test
    fun `p(0, 0)v(0, 2) and p(0, 0)v(0, 1) should be overlapping`() {
        val line1 = point(0, 0).withVector(0, 2)
        val line2 = point(0, 0).withVector(0, 2)
        val intersection = line1.intersectionTime(line2)
        assertEquals(Intersection.Overlap, intersection)
    }

    @Test
    fun `p(1, 1)v(1, 0) and p(0_5, 1)v(0, 2) should not be intersecting`() {
        val line1 = point(1, 1).withVector(1, 0)
        val line2 = point(0.5, 1.0).withVector(0, 2)
        val intersection = line1.intersectionTime(line2)
        assertEquals(Intersection.Empty, intersection)
    }

    @Test
    internal fun `p(0, 0)v(1, 0) and p(0, 0)v(0, 1) should not be considered intersecting`() {
        val line1 = point(0, 0).withVector(1, 0)
        val line2 = point(1, 0).withVector(0, -1)
        val intersection = line1.intersectionTime(line2, inclusive = false)
        assertEquals(Intersection.Empty, intersection)
    }
}