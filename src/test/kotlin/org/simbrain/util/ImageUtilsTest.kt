package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class ImageUtilsTest {

    private fun draw(values: DoubleArray, bounds: ClosedFloatingPointRange<Double>? = null): List<Int> {
        val image = BufferedImage(values.size, 1, BufferedImage.TYPE_INT_RGB)
        if (bounds == null) {
            values.writeSimbrainColorImage(image, neg = Color.BLUE, mid = Color.WHITE, pos = Color.RED)
        } else {
            values.writeSimbrainColorImage(image, bounds, neg = Color.BLUE, mid = Color.WHITE, pos = Color.RED)
        }
        return values.indices.map { image.getRGB(it, 0) }
    }

    @Test
    fun `values beyond the default bounds draw alike and wider bounds tell them apart`() {
        val (two, three) = draw(doubleArrayOf(2.0, 3.0))
        assertEquals(two, three)
        assertEquals(Color.RED.rgb, two)

        val (twoOfFive, threeOfFive) = draw(doubleArrayOf(2.0, 3.0), -1.0..5.0)
        assertNotEquals(twoOfFive, threeOfFive)
    }

    @Test
    fun `each side of zero is scaled by its own bound`() {
        val (low, zero, high) = draw(doubleArrayOf(-2.0, 0.0, 10.0), -2.0..10.0)
        assertEquals(Color.BLUE.rgb, low)
        assertEquals(Color.WHITE.rgb, zero)
        assertEquals(Color.RED.rgb, high)
    }

    @Test
    fun `a side whose bound stops at zero draws as the midpoint`() {
        val (negative, positive) = draw(doubleArrayOf(-0.5, 1.0), 0.0..1.0)
        assertEquals(Color.WHITE.rgb, negative)
        assertEquals(Color.RED.rgb, positive)
    }

    @Test
    fun `explicit default bounds match the unbounded call`() {
        val values = doubleArrayOf(-1.5, -0.3, 0.0, 0.4, 0.9, 7.0)
        assertEquals(draw(values), draw(values, -1.0..1.0))
    }
}
