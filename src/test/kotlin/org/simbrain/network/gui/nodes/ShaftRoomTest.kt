package org.simbrain.network.gui.nodes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.p
import java.awt.geom.CubicCurve2D

class ShaftRoomTest {

    private val margin = ShaftRoom.MARGIN

    /** Shaft of an arrow from the top edge of a layer at the origin to a layer 200 above it. */
    private fun upwardShaft() = CubicCurve2D.Double(0.0, 0.0, 0.0, -100.0, 0.0, -100.0, 0.0, -200.0)

    @Test
    fun `between stacked layers only the height is limited and the drawing is centered with what accompanies it`() {
        val room = shaftRoom(upwardShaft(), above = 20.0, below = 10.0)
        assertEquals(200.0 - 30.0 - 2 * margin, room.height, 1e-9)
        assertEquals(Double.POSITIVE_INFINITY, room.width)
        assertEquals(0.0, room.centerX, 1e-9)
        // Shifted down by half the difference between what is above and what is below
        assertEquals(-100.0 + 5.0, room.centerY, 1e-9)
    }

    @Test
    fun `between layers side by side only the width is limited`() {
        val shaft = CubicCurve2D.Double(0.0, 0.0, 150.0, 0.0, 150.0, 0.0, 300.0, 0.0)
        val room = shaftRoom(shaft, above = 20.0, below = 10.0, beside = 8.0)
        assertEquals(300.0 - 16.0 - 2 * margin, room.width, 1e-9)
        assertEquals(Double.POSITIVE_INFINITY, room.height)
        assertEquals(150.0, room.centerX, 1e-9)
        assertEquals(0.0, room.centerY, 1e-9)
    }

    @Test
    fun `layers offset sideways are still measured across the gap their facing edges leave`() {
        // Leaves a top edge and arrives at a bottom edge 100 above it and 400 to the right
        val shaft = CubicCurve2D.Double(0.0, 0.0, 0.0, -200.0, 400.0, 100.0, 400.0, -100.0)
        val room = shaftRoom(shaft)
        assertEquals(100.0 - 2 * margin, room.height, 1e-9)
        assertEquals(Double.POSITIVE_INFINITY, room.width)
    }

    @Test
    fun `a shared gap gives each connector the half nearer its source`() {
        val room = shaftRoom(upwardShaft(), position = 0.25, shared = true)
        assertEquals(100.0 - 2 * margin, room.height, 1e-9)
        assertEquals(-50.0, room.centerY, 1e-9)
    }

    @Test
    fun `an arrow that turns a corner is limited by one layer in each direction and stays on the shaft`() {
        // Leaves a top edge and arrives at the left edge of a layer up and to the right
        val shaft = CubicCurve2D.Double(0.0, 0.0, 0.0, -100.0, 50.0, -150.0, 150.0, -150.0)
        val onShaft = shaft.p(0.5)
        val room = shaftRoom(shaft, below = 10.0, beside = 4.0)
        assertEquals(onShaft.x, room.centerX, 1e-9)
        assertEquals(onShaft.y, room.centerY, 1e-9)
        assertEquals(2 * (0.0 - onShaft.y - 10.0 - margin), room.height, 1e-9)
        assertEquals(2 * (150.0 - onShaft.x - 4.0 - margin), room.width, 1e-9)
    }

    @Test
    fun `a fill fraction leaves the rest of the gap to the arrow`() {
        val room = shaftRoom(upwardShaft(), above = 20.0, below = 10.0, fill = 0.6)
        // What accompanies the drawing shares its fraction of the gap
        assertEquals(200.0 * 0.6 - 30.0, room.height, 1e-9)
        assertEquals(-100.0 + 5.0, room.centerY, 1e-9)
    }

    @Test
    fun `a fill fraction holds a drawing limited on one side to its share of the distance to that side`() {
        val shaft = CubicCurve2D.Double(0.0, 0.0, 0.0, -100.0, 50.0, -150.0, 150.0, -150.0)
        val onShaft = shaft.p(0.5)
        val room = shaftRoom(shaft, below = 10.0, fill = 0.5)
        assertEquals(2 * ((0.0 - onShaft.y) * 0.5 - 10.0), room.height, 1e-9)
        assertEquals(onShaft.y, room.centerY, 1e-9)
    }

    @Test
    fun `a square drawing takes the smaller of the two extents within its limits`() {
        assertEquals(50.0, ShaftRoom(50.0, Double.POSITIVE_INFINITY, 0.0, 0.0).squareSide(20.0, 100.0))
        assertEquals(100.0, ShaftRoom(300.0, 400.0, 0.0, 0.0).squareSide(20.0, 100.0))
        assertEquals(20.0, ShaftRoom(-30.0, 400.0, 0.0, 0.0).squareSide(20.0, 100.0))
    }

    @Test
    fun `a shaft with no length leaves no room`() {
        val room = shaftRoom(CubicCurve2D.Double(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0))
        assertTrue(room.width <= 0.0 && room.height <= 0.0)
    }
}
