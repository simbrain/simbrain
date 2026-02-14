package org.simbrain.network.conv

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.TensorShape

class ConvOpsTest {

    @Test
    fun `identity conv with 1x1 kernel`() {
        // 1x1 conv with 1 filter on a single-channel 3x3 input should just scale by kernel weight
        val inputShape = TensorShape(3, 3, 1)
        val input = doubleArrayOf(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )
        val outputShape = TensorShape(3, 3, 1)
        val output = DoubleArray(outputShape.size)
        val kernels = doubleArrayOf(2.0) // single 1x1 kernel, weight = 2
        val biases = doubleArrayOf(0.0)

        ConvOps.conv2d(input, inputShape, kernels, 1, 1, biases, output, outputShape, 1, 0, 0)

        assertArrayEquals(
            doubleArrayOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0),
            output, 1e-10
        )
    }

    @Test
    fun `conv2d with known 3x3 kernel`() {
        // 3x3 input, 3x3 kernel, VALID padding, no bias
        val inputShape = TensorShape(3, 3, 1)
        val input = doubleArrayOf(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )
        val outputShape = TensorShape(1, 1, 1)
        val output = DoubleArray(1)
        // All-ones kernel = sum of all elements
        val kernels = DoubleArray(9) { 1.0 }
        val biases = doubleArrayOf(0.0)

        ConvOps.conv2d(input, inputShape, kernels, 1, 3, biases, output, outputShape, 1, 0, 0)

        assertEquals(45.0, output[0], 1e-10)
    }

    @Test
    fun `conv2d with bias`() {
        val inputShape = TensorShape(3, 3, 1)
        val input = DoubleArray(9) { 0.0 }
        val outputShape = TensorShape(1, 1, 1)
        val output = DoubleArray(1)
        val kernels = DoubleArray(9) { 1.0 }
        val biases = doubleArrayOf(5.0)

        ConvOps.conv2d(input, inputShape, kernels, 1, 3, biases, output, outputShape, 1, 0, 0)

        assertEquals(5.0, output[0], 1e-10) // Only bias contributes
    }

    @Test
    fun `max pool 2x2`() {
        val inputShape = TensorShape(4, 4, 1)
        val input = doubleArrayOf(
            1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )
        val outputShape = TensorShape(2, 2, 1)
        val output = DoubleArray(outputShape.size)
        val maxIndices = IntArray(outputShape.size)

        ConvOps.maxPool2d(input, inputShape, output, outputShape, 2, 2, maxIndices)

        assertEquals(6.0, output[0], 1e-10)   // max of [1,2,5,6]
        assertEquals(8.0, output[1], 1e-10)   // max of [3,4,7,8]
        assertEquals(14.0, output[2], 1e-10)  // max of [9,10,13,14]
        assertEquals(16.0, output[3], 1e-10)  // max of [11,12,15,16]
    }

    @Test
    fun `avg pool 2x2`() {
        val inputShape = TensorShape(4, 4, 1)
        val input = doubleArrayOf(
            1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )
        val outputShape = TensorShape(2, 2, 1)
        val output = DoubleArray(outputShape.size)

        ConvOps.avgPool2d(input, inputShape, output, outputShape, 2, 2)

        assertEquals(3.5, output[0], 1e-10)   // avg of [1,2,5,6]
        assertEquals(5.5, output[1], 1e-10)   // avg of [3,4,7,8]
        assertEquals(11.5, output[2], 1e-10)  // avg of [9,10,13,14]
        assertEquals(13.5, output[3], 1e-10)  // avg of [11,12,15,16]
    }

    @Test
    fun `conv2d multi-channel input`() {
        // 2x2 input with 2 channels, 1 filter, kernel 2x2, VALID
        val inputShape = TensorShape(2, 2, 2) // HWC
        // Layout: [h=0,w=0,c=0], [h=0,w=0,c=1], [h=0,w=1,c=0], [h=0,w=1,c=1], ...
        val input = doubleArrayOf(
            1.0, 10.0,  // (0,0) ch0=1, ch1=10
            2.0, 20.0,  // (0,1) ch0=2, ch1=20
            3.0, 30.0,  // (1,0) ch0=3, ch1=30
            4.0, 40.0   // (1,1) ch0=4, ch1=40
        )
        val outputShape = TensorShape(1, 1, 1)
        val output = DoubleArray(1)
        // Kernel: 1 filter, 2 input channels, 2x2 each
        // filter0_ch0: all 1s, filter0_ch1: all 0.1
        val kernels = doubleArrayOf(
            // ch0: 2x2
            1.0, 1.0, 1.0, 1.0,
            // ch1: 2x2
            0.1, 0.1, 0.1, 0.1
        )
        val biases = doubleArrayOf(0.0)

        ConvOps.conv2d(input, inputShape, kernels, 1, 2, biases, output, outputShape, 1, 0, 0)

        // ch0 sum = 1+2+3+4 = 10, ch1 sum = (10+20+30+40)*0.1 = 10
        assertEquals(20.0, output[0], 1e-10)
    }

    @Test
    fun `max pool preserves channels`() {
        // 4x4 with 2 channels, pool 2x2 -> 2x2 with 2 channels
        val inputShape = TensorShape(4, 4, 2)
        val input = DoubleArray(inputShape.size)
        // Fill ch0 with sequential, ch1 with negative sequential
        for (h in 0 until 4) {
            for (w in 0 until 4) {
                val v = (h * 4 + w + 1).toDouble()
                input[inputShape.index(h, w, 0)] = v
                input[inputShape.index(h, w, 1)] = -v
            }
        }
        val outputShape = TensorShape(2, 2, 2)
        val output = DoubleArray(outputShape.size)

        ConvOps.maxPool2d(input, inputShape, output, outputShape, 2, 2)

        // ch0: max of [1,2,5,6] = 6
        assertEquals(6.0, output[outputShape.index(0, 0, 0)], 1e-10)
        // ch1: max of [-1,-2,-5,-6] = -1
        assertEquals(-1.0, output[outputShape.index(0, 0, 1)], 1e-10)
    }
}
