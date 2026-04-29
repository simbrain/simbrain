package org.simbrain.world.odorworld.behaviors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SteeringMathTest {

    @Test
    fun `shortestAngleDelta of equal headings is zero`() {
        assertEquals(0.0, shortestAngleDelta(42.0, 42.0), 0.001)
    }

    @Test
    fun `shortestAngleDelta forward across zero takes the short way`() {
        // From 350 to 10 should turn +20, not -340.
        assertEquals(20.0, shortestAngleDelta(350.0, 10.0), 0.001)
    }

    @Test
    fun `shortestAngleDelta backward across zero takes the short way`() {
        // From 10 to 350 should turn -20, not +340.
        assertEquals(-20.0, shortestAngleDelta(10.0, 350.0), 0.001)
    }

    @Test
    fun `shortestAngleDelta of 90 degrees forward`() {
        assertEquals(90.0, shortestAngleDelta(0.0, 90.0), 0.001)
    }

    @Test
    fun `shortestAngleDelta of 90 degrees backward`() {
        assertEquals(-90.0, shortestAngleDelta(90.0, 0.0), 0.001)
    }

    @Test
    fun `shortestAngleDelta of full circle is zero`() {
        // 360 is the same heading as 0.
        assertEquals(0.0, shortestAngleDelta(0.0, 360.0), 0.001)
        assertEquals(0.0, shortestAngleDelta(45.0, 405.0), 0.001)
    }

    @Test
    fun `shortestAngleDelta of opposite headings is plus or minus 180`() {
        // 180 apart is ambiguous — either direction is equally short. The exact sign
        // is implementation-defined; we just assert magnitude is 180.
        val d = shortestAngleDelta(0.0, 180.0)
        assertEquals(180.0, kotlin.math.abs(d), 0.001)
    }
}
