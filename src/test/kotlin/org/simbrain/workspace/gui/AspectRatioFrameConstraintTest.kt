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
        val result = constrainFrameBounds(Rectangle(0, 0, 410, 160), chrome, 2.0, SwingConstants.EAST)
        assertEquals(Rectangle(0, 0, 410, 260), result)
    }

    @Test
    fun `south drag derives frame width from the content height`() {
        val result = constrainFrameBounds(Rectangle(0, 0, 210, 260), chrome, 2.0, SwingConstants.SOUTH)
        assertEquals(Rectangle(0, 0, 410, 260), result)
    }

    @Test
    fun `west drag keeps the right edge anchored`() {
        val result = constrainFrameBounds(Rectangle(0, 50, 310, 160), chrome, 2.0, SwingConstants.WEST)
        assertEquals(310, result.x + result.width)
        assertEquals(Rectangle(0, 50, 310, 210), result)
    }

    @Test
    fun `north drag keeps the bottom edge anchored`() {
        val result = constrainFrameBounds(Rectangle(100, 0, 210, 260), chrome, 2.0, SwingConstants.NORTH)
        assertEquals(260, result.y + result.height)
        assertEquals(Rectangle(100, 0, 410, 260), result)
    }

    @Test
    fun `corner drag fits inside the proposed rectangle so the axis that changed less wins`() {
        val mostlyDown = constrainFrameBounds(Rectangle(0, 0, 220, 300), chrome, 2.0, SwingConstants.SOUTH_EAST)
        assertEquals(Rectangle(0, 0, 220, 165), mostlyDown)
        val mostlyRight = constrainFrameBounds(Rectangle(0, 0, 500, 170), chrome, 2.0, SwingConstants.SOUTH_EAST)
        assertEquals(Rectangle(0, 0, 230, 170), mostlyRight)
    }

    @Test
    fun `north west corner drag anchors both far edges`() {
        val result = constrainFrameBounds(Rectangle(0, 90, 310, 170), chrome, 2.0, SwingConstants.NORTH_WEST)
        assertEquals(310, result.x + result.width)
        assertEquals(260, result.y + result.height)
        assertEquals(Rectangle(80, 90, 230, 170), result)
    }

    @Test
    fun `corner drag steps along one axis settle instead of alternating`() {
        var frame = Rectangle(0, 0, 410, 260)
        val results = (1..6).map { step ->
            val proposed = Rectangle(0, 0, 410 + step * 20, 260 + 2)
            frame = constrainFrameBounds(proposed, chrome, 2.0, SwingConstants.SOUTH_EAST)
            frame
        }
        assertEquals(1, results.distinct().size)
        assertEquals(Rectangle(0, 0, 414, 262), results.first())
    }

    @Test
    fun `unknown direction shrinks the taller axis to fit`() {
        val result = constrainFrameBounds(Rectangle(20, 30, 310, 400), chrome, 2.0)
        assertEquals(Rectangle(20, 30, 310, 210), result)
    }

    @Test
    fun `degenerate proposals are returned unchanged`() {
        val proposed = Rectangle(0, 0, 5, 160)
        assertEquals(proposed, constrainFrameBounds(proposed, chrome, 2.0, SwingConstants.WEST))
    }
}
