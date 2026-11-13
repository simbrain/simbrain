/**
 * Placement of a connector's drawing on its arrow: its size with the arrow to itself, sliding along the arrow to get
 * clear of an obstacle, and shrinking only where nowhere on the arrow has room.
 */
package org.simbrain.network.gui.nodes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.geom.CubicCurve2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs

class ShaftPlacementTest {

    /** A weight matrix image with a label box above it and a caption below. */
    private val image = ShaftDrawing(
        above = 20.0, below = 10.0, minWidth = 90.0, minSide = 30.0, maxSide = 180.0, comfortable = 90.0, fill = 0.6
    )

    private val label = ShaftDrawing(above = 8.0, below = 8.0, minWidth = 100.0)

    /** Shaft of an arrow from the top edge of a layer at the origin straight up to a layer [gap] above it. */
    private fun upwardShaft(gap: Double) = CubicCurve2D.Double(0.0, 0.0, 0.0, -gap / 2, 0.0, -gap / 2, 0.0, -gap)

    /** The layer an upward shaft leaves, whose top edge is at the origin. */
    private val layerBelow = Rectangle2D.Double(-60.0, 0.0, 120.0, 44.0)

    /** The layer an upward shaft of [gap] arrives at, beyond its arrowhead. */
    private fun layerAbove(gap: Double) = Rectangle2D.Double(-60.0, -gap - 40.0 - 44.0, 120.0, 44.0)

    private fun ShaftPlacement.footprint(drawing: ShaftDrawing) = drawing.footprintAt(centerX, centerY, side)

    private fun Rectangle2D.grownBy(margin: Double) =
        Rectangle2D.Double(x - margin, y - margin, width + 2 * margin, height + 2 * margin)

    @Test
    fun `between layers that face each other a drawing is sized and centered as the gap between them allows`() {
        // Gaps the drawing fills outright, gaps where the fill fraction holds it back, and gaps it is largest in
        for (gap in listOf(100.0, 140.0, 200.0, 260.0, 400.0, 900.0)) {
            val shaft = upwardShaft(gap)
            val layers = listOf(layerBelow, layerAbove(gap))
            val placement = ShaftPlacer.place(shaft, image, layers = layers, ownLayers = layers)

            val room = shaftRoom(
                shaft, above = image.above, below = image.below, fill = image.fill, comfortable = image.comfortable
            )
            assertEquals(room.squareSide(image.minSide, image.maxSide), placement.side, 0.5, "side with a gap of $gap")
            assertEquals(room.centerX, placement.centerX, 0.5, "center with a gap of $gap")
            assertEquals(room.centerY, placement.centerY, 1.0, "center with a gap of $gap")
        }
    }

    @Test
    fun `layers offset diagonally leave a drawing more room than the gap between their facing edges`() {
        // Leaves the right edge of one layer and arrives at the left edge of another up and to the right
        val shaft = CubicCurve2D.Double(61.0, -8.0, 150.0, -8.0, 150.0, -308.0, 234.0, -308.0)
        val layers = listOf(
            Rectangle2D.Double(-61.0, -38.0, 122.0, 60.0), Rectangle2D.Double(239.0, -338.0, 122.0, 60.0)
        )
        val placement = ShaftPlacer.place(shaft, image, layers = layers, ownLayers = layers)

        val acrossTheGap = shaftRoom(shaft, above = 20.0, below = 10.0, fill = 0.6, comfortable = 90.0)
            .squareSide(image.minSide, image.maxSide)
        assertTrue(placement.side > acrossTheGap + 30.0, "side ${placement.side}, across the gap $acrossTheGap")
        layers.forEach { assertFalse(placement.footprint(image).intersects(it), "overlaps $it") }
    }

    @Test
    fun `a drawing slides along its arrow to get clear of an obstacle and keeps its size`() {
        val shaft = upwardShaft(900.0)
        val layers = listOf(layerBelow, layerAbove(900.0))
        val alone = ShaftPlacer.place(shaft, image, layers = layers, ownLayers = layers)
        // A layer the arrow passes behind, over the middle of the shaft
        val inTheWay = Rectangle2D.Double(-60.0, -480.0, 120.0, 60.0)

        val placement = ShaftPlacer.place(shaft, image, layers = layers + inTheWay, ownLayers = layers)

        assertEquals(alone.side, placement.side, 1e-6)
        assertFalse(placement.footprint(image).grownBy(ShaftPlacer.MARGIN - 0.5).intersects(inTheWay))
        // No further than it has to go
        val gap = minOf(
            abs(placement.footprint(image).maxY - inTheWay.y), abs(placement.footprint(image).y - inTheWay.maxY)
        )
        assertEquals(ShaftPlacer.MARGIN, gap, 1.0)
    }

    @Test
    fun `a drawing shrinks only when nowhere on its arrow has room for it`() {
        val shaft = upwardShaft(400.0)
        val layers = listOf(layerBelow, layerAbove(400.0))
        val alone = ShaftPlacer.place(shaft, image, layers = layers, ownLayers = layers)
        val inTheWay = Rectangle2D.Double(-60.0, -230.0, 120.0, 60.0)

        val placement = ShaftPlacer.place(shaft, image, layers = layers + inTheWay, ownLayers = layers)

        assertTrue(placement.side < alone.side, "side ${placement.side}, alone ${alone.side}")
        assertTrue(placement.side > image.minSide, "side ${placement.side}")
        (layers + inTheWay).forEach { assertFalse(placement.footprint(image).intersects(it), "overlaps $it") }
    }

    @Test
    fun `a drawing with no room anywhere keeps its smallest size`() {
        val shaft = upwardShaft(120.0)
        val layers = listOf(layerBelow, layerAbove(120.0))
        val covering = Rectangle2D.Double(-200.0, -200.0, 400.0, 250.0)

        val placement = ShaftPlacer.place(shaft, image, layers = layers + covering, ownLayers = layers)

        assertEquals(image.minSide, placement.side, 1e-9)
    }

    @Test
    fun `a drawing pushed along its arrow by a moving obstacle moves with it rather than in jumps`() {
        val shaft = upwardShaft(900.0)
        val layers = listOf(layerBelow, layerAbove(900.0))
        var previous: ShaftPlacement? = null
        // The obstacle approaches the middle of the shaft from below and stops short of passing it
        for (top in generateSequence(-250.0) { it - 2.0 }.takeWhile { it >= -380.0 }) {
            val inTheWay = Rectangle2D.Double(-60.0, top, 120.0, 60.0)
            val placement = ShaftPlacer.place(shaft, image, layers = layers + inTheWay, ownLayers = layers)
            previous?.let {
                assertTrue(abs(placement.centerY - it.centerY) <= 3.0, "moved from ${it.centerY} to ${placement.centerY}")
                assertEquals(it.side, placement.side, 0.5)
            }
            previous = placement
        }
    }

    @Test
    fun `a drawing that shares its gap keeps to the half nearer its source`() {
        val shaft = upwardShaft(400.0)
        val layers = listOf(layerBelow, layerAbove(400.0))
        val placement = ShaftPlacer.place(
            shaft, image, layers = layers, ownLayers = layers, preferred = 0.25, shared = true
        )

        val room = shaftRoom(
            shaft, position = 0.25, shared = true, above = image.above, below = image.below,
            fill = image.fill, comfortable = image.comfortable
        )
        assertEquals(room.squareSide(image.minSide, image.maxSide), placement.side, 0.5)
        assertTrue(placement.footprint(image).y >= -200.0, "footprint ${placement.footprint(image)}")
    }

    @Test
    fun `a label box slides clear of an earlier connector's drawing`() {
        val shaft = upwardShaft(400.0)
        val layers = listOf(layerBelow, layerAbove(400.0))
        val middle = ShaftPlacer.place(shaft, label, layers = layers, ownLayers = layers)
        assertEquals(-200.0, middle.centerY, 1.0)
        val earlier = Rectangle2D.Double(-40.0, -240.0, 80.0, 80.0)

        val placement = ShaftPlacer.place(shaft, label, layers = layers, drawings = listOf(earlier), ownLayers = layers)

        assertFalse(placement.footprint(label).intersects(earlier))
        assertEquals(0.0, placement.side, 1e-9)
    }

    @Test
    fun `a label box wider than the gap between layers side by side still lets the image fill the gap`() {
        val shaft = CubicCurve2D.Double(0.0, 0.0, 40.0, 0.0, 40.0, 0.0, 80.0, 0.0)
        val layers = listOf(Rectangle2D.Double(-120.0, -22.0, 120.0, 44.0), Rectangle2D.Double(120.0, -22.0, 120.0, 44.0))

        val placement = ShaftPlacer.place(shaft, image, layers = layers, ownLayers = layers)

        assertEquals(80.0 - 2 * ShaftPlacer.MARGIN, placement.side, 0.5)
    }
}
