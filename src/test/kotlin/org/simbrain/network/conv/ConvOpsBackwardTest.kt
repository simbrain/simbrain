package org.simbrain.network.conv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.TensorShape

/**
 * Numerical gradient checking for ConvOps backward functions.
 */
class ConvOpsBackwardTest {

    private val eps = 1e-5
    private val tol = 1e-4

    @Test
    fun `conv2dBackwardKernels matches numerical gradient`() {
        val inputShape = TensorShape(4, 4, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() / 16.0 }
        val numFilters = 2
        val kernelSize = 3
        val stride = 1
        val padH = 1
        val padW = 1
        val outputShape = TensorShape(4, 4, numFilters)
        val kernelArea = kernelSize * kernelSize
        val kernels = DoubleArray(numFilters * 1 * kernelArea) { (it + 1).toDouble() * 0.1 }
        val biases = DoubleArray(numFilters) { 0.0 }

        // Random output gradient
        val outputGrad = DoubleArray(outputShape.size) { (it % 7 - 3).toDouble() * 0.1 }

        // Analytical gradient
        val kernelGrad = DoubleArray(kernels.size)
        val biasGrad = DoubleArray(numFilters)
        ConvOps.conv2dBackwardKernels(
            outputGrad, outputShape, input, inputShape,
            kernelGrad, numFilters, kernelSize, biasGrad,
            stride, padH, padW
        )

        // Numerical gradient for kernels
        for (k in kernels.indices) {
            val orig = kernels[k]

            kernels[k] = orig + eps
            val outputPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outputPlus, outputShape, stride, padH, padW)
            var lossPlus = 0.0
            for (i in outputGrad.indices) lossPlus += outputPlus[i] * outputGrad[i]

            kernels[k] = orig - eps
            val outputMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outputMinus, outputShape, stride, padH, padW)
            var lossMinus = 0.0
            for (i in outputGrad.indices) lossMinus += outputMinus[i] * outputGrad[i]

            kernels[k] = orig
            val numGrad = (lossPlus - lossMinus) / (2.0 * eps)
            assertEquals(numGrad, kernelGrad[k], tol, "Kernel grad mismatch at index $k")
        }

        // Numerical gradient for biases
        for (b in biases.indices) {
            val orig = biases[b]

            biases[b] = orig + eps
            val outputPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outputPlus, outputShape, stride, padH, padW)
            var lossPlus = 0.0
            for (i in outputGrad.indices) lossPlus += outputPlus[i] * outputGrad[i]

            biases[b] = orig - eps
            val outputMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outputMinus, outputShape, stride, padH, padW)
            var lossMinus = 0.0
            for (i in outputGrad.indices) lossMinus += outputMinus[i] * outputGrad[i]

            biases[b] = orig
            val numGrad = (lossPlus - lossMinus) / (2.0 * eps)
            assertEquals(numGrad, biasGrad[b], tol, "Bias grad mismatch at index $b")
        }
    }

    @Test
    fun `conv2dBackwardInput matches numerical gradient`() {
        val inputShape = TensorShape(4, 4, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() / 16.0 }
        val numFilters = 2
        val kernelSize = 3
        val stride = 1
        val padH = 1
        val padW = 1
        val outputShape = TensorShape(4, 4, numFilters)
        val kernelArea = kernelSize * kernelSize
        val kernels = DoubleArray(numFilters * 1 * kernelArea) { (it + 1).toDouble() * 0.1 }
        val biases = DoubleArray(numFilters) { 0.0 }

        val outputGrad = DoubleArray(outputShape.size) { (it % 7 - 3).toDouble() * 0.1 }

        // Analytical gradient
        val inputGrad = DoubleArray(inputShape.size)
        ConvOps.conv2dBackwardInput(
            outputGrad, outputShape, kernels, numFilters, kernelSize,
            inputGrad, inputShape, stride, padH, padW
        )

        // Numerical gradient
        for (k in input.indices) {
            val orig = input[k]

            input[k] = orig + eps
            val outputPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outputPlus, outputShape, stride, padH, padW)
            var lossPlus = 0.0
            for (i in outputGrad.indices) lossPlus += outputPlus[i] * outputGrad[i]

            input[k] = orig - eps
            val outputMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outputMinus, outputShape, stride, padH, padW)
            var lossMinus = 0.0
            for (i in outputGrad.indices) lossMinus += outputMinus[i] * outputGrad[i]

            input[k] = orig
            val numGrad = (lossPlus - lossMinus) / (2.0 * eps)
            assertEquals(numGrad, inputGrad[k], tol, "Input grad mismatch at index $k")
        }
    }

    @Test
    fun `conv2dBackwardInput multi-channel matches numerical gradient`() {
        val inputShape = TensorShape(3, 3, 2)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() / 18.0 }
        val numFilters = 2
        val kernelSize = 3
        val stride = 1
        val padH = 1
        val padW = 1
        val outputShape = TensorShape(3, 3, numFilters)
        val kernelArea = kernelSize * kernelSize
        val kernels = DoubleArray(numFilters * 2 * kernelArea) { (it + 1).toDouble() * 0.05 }
        val biases = DoubleArray(numFilters) { 0.0 }

        val outputGrad = DoubleArray(outputShape.size) { (it % 5 - 2).toDouble() * 0.1 }

        val inputGrad = DoubleArray(inputShape.size)
        ConvOps.conv2dBackwardInput(
            outputGrad, outputShape, kernels, numFilters, kernelSize,
            inputGrad, inputShape, stride, padH, padW
        )

        for (k in input.indices) {
            val orig = input[k]

            input[k] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            input[k] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            input[k] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, inputGrad[k], tol, "Multi-channel input grad mismatch at index $k")
        }
    }

    @Test
    fun `conv backward gradients match numerical gradient with stride 2 and SAME-like padding`() {
        val inputShape = TensorShape(5, 5, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() / 25.0 }
        val numFilters = 2
        val kernelSize = 3
        val stride = 2
        val padH = 1
        val padW = 1
        val outputShape = TensorShape(3, 3, numFilters)
        val kernelArea = kernelSize * kernelSize
        val kernels = DoubleArray(numFilters * inputShape.channels * kernelArea) { (it + 1).toDouble() * 0.03 }
        val biases = DoubleArray(numFilters) { 0.0 }
        val outputGrad = DoubleArray(outputShape.size) { (it % 7 - 3).toDouble() * 0.1 }

        val kernelGrad = DoubleArray(kernels.size)
        val biasGrad = DoubleArray(numFilters)
        ConvOps.conv2dBackwardKernels(
            outputGrad, outputShape,
            input, inputShape,
            kernelGrad, numFilters, kernelSize,
            biasGrad,
            stride, padH, padW
        )

        val inputGrad = DoubleArray(inputShape.size)
        ConvOps.conv2dBackwardInput(
            outputGrad, outputShape,
            kernels, numFilters, kernelSize,
            inputGrad, inputShape,
            stride, padH, padW
        )

        // Numerical gradient check for kernels
        for (k in kernels.indices) {
            val orig = kernels[k]
            kernels[k] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            kernels[k] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            kernels[k] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, kernelGrad[k], tol, "Stride-2 kernel grad mismatch at index $k")
        }

        // Numerical gradient check for biases
        for (b in biases.indices) {
            val orig = biases[b]
            biases[b] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            biases[b] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            biases[b] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, biasGrad[b], tol, "Stride-2 bias grad mismatch at index $b")
        }

        // Numerical gradient check for input
        for (k in input.indices) {
            val orig = input[k]

            input[k] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            input[k] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            input[k] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, inputGrad[k], tol, "Stride-2 input grad mismatch at index $k")
        }
    }

    @Test
    fun `maxPool2dBackward routes gradient to max positions`() {
        val inputShape = TensorShape(4, 4, 1)
        val input = doubleArrayOf(
            1.0, 3.0, 2.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )
        val outputShape = TensorShape(2, 2, 1)
        val output = DoubleArray(outputShape.size)
        val maxIndices = IntArray(outputShape.size)

        ConvOps.maxPool2d(input, inputShape, output, outputShape, 2, 2, maxIndices)

        // Output should be max of each 2x2 block
        assertEquals(6.0, output[0])
        assertEquals(8.0, output[1])
        assertEquals(14.0, output[2])
        assertEquals(16.0, output[3])

        // Backward
        val outputGrad = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val inputGrad = DoubleArray(inputShape.size)
        ConvOps.maxPool2dBackward(outputGrad, outputShape, maxIndices, inputGrad)

        // Gradient should only go to max positions
        // Block(0,0): max=6 at (1,1)
        assertEquals(0.0, inputGrad[inputShape.index(0, 0, 0)]) // 1.0 not max
        assertEquals(1.0, inputGrad[inputShape.index(1, 1, 0)]) // 6.0 was max of block 0
        // Block(0,1): max=8 at (1,3)
        assertEquals(2.0, inputGrad[inputShape.index(1, 3, 0)]) // 8.0 was max of block 1
        // Block(1,0): max=14 at (3,1)
        assertEquals(3.0, inputGrad[inputShape.index(3, 1, 0)]) // 14.0 was max of block 2
        // Block(1,1): max=16 at (3,3)
        assertEquals(4.0, inputGrad[inputShape.index(3, 3, 0)]) // 16.0 was max of block 3
    }

    @Test
    fun `conv2dBackwardKernels multi-channel matches numerical gradient`() {
        val inputShape = TensorShape(3, 3, 2)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() / 18.0 }
        val numFilters = 2
        val kernelSize = 3
        val stride = 1
        val padH = 1
        val padW = 1
        val outputShape = TensorShape(3, 3, numFilters)
        val kernelArea = kernelSize * kernelSize
        val kernels = DoubleArray(numFilters * 2 * kernelArea) { (it + 1).toDouble() * 0.05 }
        val biases = DoubleArray(numFilters) { 0.0 }

        val outputGrad = DoubleArray(outputShape.size) { (it % 5 - 2).toDouble() * 0.1 }

        // Analytical gradient
        val kernelGrad = DoubleArray(kernels.size)
        val biasGrad = DoubleArray(numFilters)
        ConvOps.conv2dBackwardKernels(
            outputGrad, outputShape, input, inputShape,
            kernelGrad, numFilters, kernelSize, biasGrad,
            stride, padH, padW
        )

        // Numerical gradient for kernels
        for (k in kernels.indices) {
            val orig = kernels[k]

            kernels[k] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            kernels[k] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            kernels[k] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, kernelGrad[k], tol, "Multi-channel kernel grad mismatch at index $k")
        }
    }

    @Test
    fun `conv backward VALID padding matches numerical gradient`() {
        val inputShape = TensorShape(5, 5, 1)
        val input = DoubleArray(inputShape.size) { (it + 1).toDouble() / 25.0 }
        val numFilters = 1
        val kernelSize = 3
        val stride = 1
        val padH = 0
        val padW = 0
        val outputShape = TensorShape(3, 3, numFilters)
        val kernelArea = kernelSize * kernelSize
        val kernels = DoubleArray(numFilters * 1 * kernelArea) { (it + 1).toDouble() * 0.1 }
        val biases = DoubleArray(numFilters) { 0.0 }

        val outputGrad = DoubleArray(outputShape.size) { (it % 5 - 2).toDouble() * 0.1 }

        // Analytical gradients
        val kernelGrad = DoubleArray(kernels.size)
        val biasGrad = DoubleArray(numFilters)
        ConvOps.conv2dBackwardKernels(
            outputGrad, outputShape, input, inputShape,
            kernelGrad, numFilters, kernelSize, biasGrad,
            stride, padH, padW
        )
        val inputGrad = DoubleArray(inputShape.size)
        ConvOps.conv2dBackwardInput(
            outputGrad, outputShape, kernels, numFilters, kernelSize,
            inputGrad, inputShape, stride, padH, padW
        )

        // Numerical gradient check for kernels
        for (k in kernels.indices) {
            val orig = kernels[k]
            kernels[k] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            kernels[k] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            kernels[k] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, kernelGrad[k], tol, "VALID kernel grad mismatch at index $k")
        }

        // Numerical gradient check for input
        for (k in input.indices) {
            val orig = input[k]
            input[k] = orig + eps
            val outPlus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outPlus, outputShape, stride, padH, padW)
            var lPlus = 0.0
            for (i in outputGrad.indices) lPlus += outPlus[i] * outputGrad[i]

            input[k] = orig - eps
            val outMinus = DoubleArray(outputShape.size)
            ConvOps.conv2d(input, inputShape, kernels, numFilters, kernelSize, biases, outMinus, outputShape, stride, padH, padW)
            var lMinus = 0.0
            for (i in outputGrad.indices) lMinus += outMinus[i] * outputGrad[i]

            input[k] = orig
            val numGrad = (lPlus - lMinus) / (2.0 * eps)
            assertEquals(numGrad, inputGrad[k], tol, "VALID input grad mismatch at index $k")
        }
    }

    @Test
    fun `avgPool2dBackward distributes gradient evenly`() {
        val inputShape = TensorShape(4, 4, 1)
        val outputShape = TensorShape(2, 2, 1)
        val poolSize = 2
        val stride = 2

        val outputGrad = doubleArrayOf(4.0, 8.0, 12.0, 16.0)
        val inputGrad = DoubleArray(inputShape.size)

        ConvOps.avgPool2dBackward(outputGrad, outputShape, inputGrad, inputShape, poolSize, stride)

        // Each output grad should be divided by pool area (4) and distributed to all 4 positions
        assertEquals(1.0, inputGrad[inputShape.index(0, 0, 0)], tol)
        assertEquals(1.0, inputGrad[inputShape.index(0, 1, 0)], tol)
        assertEquals(1.0, inputGrad[inputShape.index(1, 0, 0)], tol)
        assertEquals(1.0, inputGrad[inputShape.index(1, 1, 0)], tol)

        assertEquals(2.0, inputGrad[inputShape.index(0, 2, 0)], tol)
        assertEquals(2.0, inputGrad[inputShape.index(0, 3, 0)], tol)

        assertEquals(3.0, inputGrad[inputShape.index(2, 0, 0)], tol)
        assertEquals(4.0, inputGrad[inputShape.index(2, 2, 0)], tol)
    }
}
