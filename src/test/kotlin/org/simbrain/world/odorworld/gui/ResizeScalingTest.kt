/**
 * Scale math for dragging an entity's resize handle: projection onto the corner diagonal, snapping, and the common
 * ratio limits across a multi-entity selection.
 */
package org.simbrain.world.odorworld.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

class ResizeScalingTest {

    private fun p(x: Double, y: Double) = Point2D.Double(x, y)

    @Test
    fun `dragging the corner outward along its diagonal grows the scale in proportion`() {
        val corner = p(16.0, 16.0)
        val scale = ResizeScaling.rawScale(1.0, corner, grabOffset = corner, cursorOffset = p(32.0, 32.0))
        assertEquals(2.0, scale, 1e-9)
    }

    @Test
    fun `the grab point need not be exactly on the corner`() {
        val corner = p(16.0, 16.0)
        val grab = p(20.0, 20.0)
        assertEquals(1.5, ResizeScaling.rawScale(1.5, corner, grab, grab), 1e-9)
        assertEquals(1.5 * 1.25, ResizeScaling.rawScale(1.5, corner, grab, p(24.0, 24.0)), 1e-9)
    }

    @Test
    fun `motion across the diagonal does not change the scale`() {
        val corner = p(16.0, 16.0)
        val scale = ResizeScaling.rawScale(1.0, corner, corner, p(26.0, 6.0))
        assertEquals(1.0, scale, 1e-9)
    }

    @Test
    fun `a top left corner grows when dragged up and left`() {
        val corner = p(-10.0, -20.0)
        val scale = ResizeScaling.rawScale(1.0, corner, corner, p(-20.0, -40.0))
        assertEquals(2.0, scale, 1e-9)
    }

    @Test
    fun `shift snaps to quarter steps and never below one step`() {
        assertEquals(1.25, ResizeScaling.snap(1.32, stepped = true, nativeCornerDistance = 20.0, viewScale = 1.0))
        assertEquals(0.25, ResizeScaling.snap(0.06, stepped = true, nativeCornerDistance = 20.0, viewScale = 1.0))
    }

    @Test
    fun `a corner within a few screen pixels of native size snaps to one`() {
        assertEquals(1.0, ResizeScaling.snap(1.2, stepped = false, nativeCornerDistance = 20.0, viewScale = 1.0))
        assertEquals(1.4, ResizeScaling.snap(1.4, stepped = false, nativeCornerDistance = 20.0, viewScale = 1.0))
    }

    @Test
    fun `the native size snap is measured on screen so zooming in narrows it`() {
        assertEquals(1.2, ResizeScaling.snap(1.2, stepped = false, nativeCornerDistance = 20.0, viewScale = 4.0))
    }

    @Test
    fun `ratio limits keep every selected entity between the minimum and the world size`() {
        val limits = ResizeScaling.ratioLimits(
            listOf(Triple(1.0, 32.0, 32.0), Triple(0.1, 96.0, 96.0)),
            worldWidth = 320.0,
            worldHeight = 640.0
        )
        assertEquals(0.5, limits.start, 1e-9)
        assertEquals(10.0, limits.endInclusive, 1e-9)
    }
}
