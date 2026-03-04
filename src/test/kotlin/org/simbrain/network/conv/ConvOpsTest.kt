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
    fun `conv2d with SAME padding preserves spatial dimensions`() {
        // 4x4 single-channel input, 3x3 kernel, stride 1, SAME padding -> 4x4 output
        val inputShape = TensorShape(4, 4, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() }
        val outputShape = TensorShape(4, 4, 1)
        val output = DoubleArray(outputShape.size)
        // All-ones kernel so we can verify the padded border sums
        val kernels = DoubleArray(9) { 1.0 }
        val biases = doubleArrayOf(0.0)

        // SAME padding for 4x4 input, 3x3 kernel, stride 1 -> padH=1, padW=1
        ConvOps.conv2d(input, inputShape, kernels, 1, 3, biases, output, outputShape, 1, 1, 1)

        // Corner (0,0): 3x3 window centered here only covers (0,0),(0,1),(1,0),(1,1) = 1+2+5+6 = 14
        assertEquals(14.0, output[outputShape.index(0, 0, 0)], 1e-10)
        // Center (1,1): full 3x3 window covers rows 0-2, cols 0-2 = 1+2+3+5+6+7+9+10+11 = 54
        assertEquals(54.0, output[outputShape.index(1, 1, 0)], 1e-10)
        // Corner (3,3): covers (2,2),(2,3),(3,2),(3,3) = 11+12+15+16 = 54
        assertEquals(54.0, output[outputShape.index(3, 3, 0)], 1e-10)
    }

    @Test
    fun `conv2d multi-filter produces independent channels`() {
        // 3x3 input, 2 filters with 3x3 kernels, VALID padding -> 1x1x2 output
        val inputShape = TensorShape(3, 3, 1)
        val input = DoubleArray(9) { 1.0 } // all ones
        val outputShape = TensorShape(1, 1, 2)
        val output = DoubleArray(outputShape.size)
        // Filter 0: all ones (sum = 9), Filter 1: all twos (sum = 18)
        val kernels = DoubleArray(18) { i -> if (i < 9) 1.0 else 2.0 }
        val biases = doubleArrayOf(0.0, 10.0)

        ConvOps.conv2d(input, inputShape, kernels, 2, 3, biases, output, outputShape, 1, 0, 0)

        assertEquals(9.0, output[outputShape.index(0, 0, 0)], 1e-10)   // 9*1 + bias 0
        assertEquals(28.0, output[outputShape.index(0, 0, 1)], 1e-10)  // 9*2 + bias 10
    }

    @Test
    fun `conv2d with stride 2`() {
        // 4x4 input, 2x2 kernel, stride 2, VALID -> 2x2 output
        val inputShape = TensorShape(4, 4, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() }
        val outputShape = TensorShape(2, 2, 1)
        val output = DoubleArray(outputShape.size)
        val kernels = DoubleArray(4) { 1.0 } // all-ones 2x2
        val biases = doubleArrayOf(0.0)

        ConvOps.conv2d(input, inputShape, kernels, 1, 2, biases, output, outputShape, 2, 0, 0)

        // Window at (0,0): 1+2+5+6 = 14
        assertEquals(14.0, output[outputShape.index(0, 0, 0)], 1e-10)
        // Window at (0,1): 3+4+7+8 = 22
        assertEquals(22.0, output[outputShape.index(0, 1, 0)], 1e-10)
        // Window at (1,0): 9+10+13+14 = 46
        assertEquals(46.0, output[outputShape.index(1, 0, 0)], 1e-10)
        // Window at (1,1): 11+12+15+16 = 54
        assertEquals(54.0, output[outputShape.index(1, 1, 0)], 1e-10)
    }

    @Test
    fun `conv2d accumulates into output`() {
        // Calling conv2d twice should accumulate, not overwrite
        val inputShape = TensorShape(3, 3, 1)
        val input = DoubleArray(9) { 1.0 }
        val outputShape = TensorShape(1, 1, 1)
        val output = DoubleArray(1)
        val kernels = DoubleArray(9) { 1.0 }
        val biases = doubleArrayOf(0.0)

        ConvOps.conv2d(input, inputShape, kernels, 1, 3, biases, output, outputShape, 1, 0, 0)
        assertEquals(9.0, output[0], 1e-10)

        // Call again without zeroing output — should accumulate
        ConvOps.conv2d(input, inputShape, kernels, 1, 3, biases, output, outputShape, 1, 0, 0)
        assertEquals(18.0, output[0], 1e-10)
    }

    @Test
    fun `avg pool multi-channel`() {
        // 4x4 with 2 channels, pool 2x2 -> 2x2 with 2 channels
        val inputShape = TensorShape(4, 4, 2)
        val input = DoubleArray(inputShape.size)
        for (h in 0 until 4) {
            for (w in 0 until 4) {
                input[inputShape.index(h, w, 0)] = (h * 4 + w + 1).toDouble()
                input[inputShape.index(h, w, 1)] = 100.0
            }
        }
        val outputShape = TensorShape(2, 2, 2)
        val output = DoubleArray(outputShape.size)

        ConvOps.avgPool2d(input, inputShape, output, outputShape, 2, 2)

        // ch0 block(0,0): avg of [1,2,5,6] = 3.5
        assertEquals(3.5, output[outputShape.index(0, 0, 0)], 1e-10)
        // ch1: all 100s -> avg = 100
        assertEquals(100.0, output[outputShape.index(0, 0, 1)], 1e-10)
        assertEquals(100.0, output[outputShape.index(1, 1, 1)], 1e-10)
    }

    @Test
    fun `pool with non-divisible input clips at boundary`() {
        // 5x5 input, pool 2x2, stride 2 -> 2x2 output (last row/col dropped)
        val inputShape = TensorShape(5, 5, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() }
        val outputShape = TensorShape(2, 2, 1)
        val output = DoubleArray(outputShape.size)

        ConvOps.maxPool2d(input, inputShape, output, outputShape, 2, 2)

        // Block(0,0): max of (0,0),(0,1),(1,0),(1,1) = max(1,2,6,7) = 7
        assertEquals(7.0, output[outputShape.index(0, 0, 0)], 1e-10)
        // Block(0,1): max of (0,2),(0,3),(1,2),(1,3) = max(3,4,8,9) = 9
        assertEquals(9.0, output[outputShape.index(0, 1, 0)], 1e-10)
        // Block(1,0): max of (2,0),(2,1),(3,0),(3,1) = max(11,12,16,17) = 17
        assertEquals(17.0, output[outputShape.index(1, 0, 0)], 1e-10)
        // Block(1,1): max of (2,2),(2,3),(3,2),(3,3) = max(13,14,18,19) = 19
        assertEquals(19.0, output[outputShape.index(1, 1, 0)], 1e-10)
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
