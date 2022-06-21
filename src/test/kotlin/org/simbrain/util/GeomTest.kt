package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.geom.Line2D

class GeomTest {

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