package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TensorShapeTest {

    @Test
    fun `index round-trip for HWC layout`() {
        val shape = TensorShape(4, 5, 3)
        val data = DoubleArray(shape.size)
        var counter = 0.0
        for (h in 0 until shape.height) {
            for (w in 0 until shape.width) {
                for (c in 0 until shape.channels) {
                    data[shape.index(h, w, c)] = counter++
                }
            }
        }
        assertEquals(0.0, data[shape.index(0, 0, 0)])
        assertEquals(1.0, data[shape.index(0, 0, 1)])
        assertEquals(2.0, data[shape.index(0, 0, 2)])
        assertEquals(3.0, data[shape.index(0, 1, 0)])
        // Last element
        assertEquals(counter - 1.0, data[shape.index(3, 4, 2)])
    }

    @Test
    fun `size is correct`() {
        val shape = TensorShape(10, 8, 3)
        assertEquals(10 * 8 * 3, shape.size)
    }

    @Test
    fun `conv output shape VALID padding`() {
        val input = TensorShape(28, 28, 1)
        val output = input.convOutputShape(kernelSize = 3, stride = 1, padding = Padding.VALID, numFilters = 16)
        assertEquals(26, output.height)
        assertEquals(26, output.width)
        assertEquals(16, output.channels)
    }

    @Test
    fun `conv output shape SAME padding stride 1`() {
        val input = TensorShape(28, 28, 1)
        val output = input.convOutputShape(kernelSize = 3, stride = 1, padding = Padding.SAME, numFilters = 8)
        assertEquals(28, output.height)
        assertEquals(28, output.width)
        assertEquals(8, output.channels)
    }

    @Test
    fun `conv output shape with stride 2`() {
        val input = TensorShape(28, 28, 3)
        val output = input.convOutputShape(kernelSize = 3, stride = 2, padding = Padding.VALID, numFilters = 32)
        assertEquals(13, output.height)
        assertEquals(13, output.width)
        assertEquals(32, output.channels)
    }

    @Test
    fun `pool output shape`() {
        val input = TensorShape(28, 28, 16)
        val output = input.poolOutputShape(poolSize = 2, stride = 2)
        assertEquals(14, output.height)
        assertEquals(14, output.width)
        assertEquals(16, output.channels) // channels preserved
    }

    @Test
    fun `pool output shape non-divisible`() {
        val input = TensorShape(7, 7, 3)
        val output = input.poolOutputShape(poolSize = 2, stride = 2)
        assertEquals(3, output.height)
        assertEquals(3, output.width)
    }

    @Test
    fun `default channel is 1`() {
        val shape = TensorShape(5, 5)
        assertEquals(1, shape.channels)
        assertEquals(25, shape.size)
    }
}
