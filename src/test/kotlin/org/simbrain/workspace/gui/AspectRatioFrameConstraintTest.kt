package org.simbrain.workspace.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.SwingConstants

class AspectRatioFrameConstraintTest {

    private val chrome = Dimension(10, 60)

    @Test
    fun `fit to box shrinks a wide ratio to the box width`() {
        assertEquals(Dimension(800, 200), fitToBox(4.0, 800, 400))
    }

    @Test
    fun `fit to box shrinks a tall ratio to the box height`() {
        assertEquals(Dimension(200, 400), fitToBox(0.5, 800, 400))
    }

    @Test
    fun `fit to box returns the box when it already has the ratio`() {
        assertEquals(Dimension(300, 200), fitToBox(1.5, 300, 200))
    }

    @Test
    fun `east drag derives frame height from the content width`() {
        val current = Rectangle(0, 0, 210, 160)
        val proposed = Rectangle(0, 0, 410, 160)
        val result = constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.EAST)
        assertEquals(Rectangle(0, 0, 410, 260), result)
    }

    @Test
    fun `south drag derives frame width from the content height`() {
        val current = Rectangle(0, 0, 210, 160)
        val proposed = Rectangle(0, 0, 210, 260)
        val result = constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.SOUTH)
        assertEquals(Rectangle(0, 0, 410, 260), result)
    }

    @Test
    fun `west drag keeps the right edge anchored`() {
        val current = Rectangle(100, 50, 210, 160)
        val proposed = Rectangle(0, 50, 310, 160)
        val result = constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.WEST)
        assertEquals(310, result.x + result.width)
        assertEquals(Rectangle(0, 50, 310, 210), result)
    }

    @Test
    fun `north drag keeps the bottom edge anchored`() {
        val current = Rectangle(100, 100, 210, 160)
        val proposed = Rectangle(100, 0, 210, 260)
        val result = constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.NORTH)
        assertEquals(260, result.y + result.height)
        assertEquals(Rectangle(100, 0, 410, 260), result)
    }

    @Test
    fun `corner drag lets the axis that moved more drive`() {
        val current = Rectangle(0, 0, 210, 160)
        val proposed = Rectangle(0, 0, 220, 300)
        val result = constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.SOUTH_EAST)
        assertEquals(Rectangle(0, 0, 490, 300), result)
    }

    @Test
    fun `north west corner drag anchors both far edges`() {
        val current = Rectangle(100, 100, 210, 160)
        val proposed = Rectangle(0, 90, 310, 170)
        val result = constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.NORTH_WEST)
        assertEquals(310, result.x + result.width)
        assertEquals(260, result.y + result.height)
        assertEquals(Rectangle(0, 50, 310, 210), result)
    }

    @Test
    fun `unknown direction with no change snaps height from width`() {
        val current = Rectangle(20, 30, 310, 400)
        val result = constrainFrameBounds(current, current, chrome, 2.0)
        assertEquals(Rectangle(20, 30, 310, 210), result)
    }

    @Test
    fun `degenerate proposals are returned unchanged`() {
        val current = Rectangle(0, 0, 210, 160)
        val proposed = Rectangle(0, 0, 5, 160)
        assertEquals(proposed, constrainFrameBounds(proposed, current, chrome, 2.0, SwingConstants.WEST))
    }
}
