/**
 * Checks the convolution routines against direct implementations of the definitions, which loop over every filter,
 * output position, channel and kernel entry in turn.
 */
package org.simbrain.network.conv

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.TensorShape
import kotlin.random.Random

class ConvOpsReferenceTest {

    private class Case(val inH: Int, val inW: Int, val inC: Int, val filters: Int, val k: Int, val stride: Int, val pad: Int) {
        val inShape = TensorShape(inH, inW, inC)
        val outShape = TensorShape((inH + 2 * pad - k) / stride + 1, (inW + 2 * pad - k) / stride + 1, filters)
    }

    private val cases = listOf(
        Case(20, 20, 1, 5, 3, 1, 1),
        Case(10, 10, 5, 8, 3, 1, 1),
        Case(7, 9, 3, 4, 3, 2, 1),
        Case(8, 8, 2, 3, 5, 1, 2),
        Case(6, 5, 4, 6, 3, 1, 0),
        Case(9, 9, 2, 1, 1, 2, 0),
    )

    /** Values in [-1, 1) with about a third of them zero, as after a ReLU. */
    private fun Random.sparse(n: Int) = DoubleArray(n) { if (nextInt(3) == 0) 0.0 else nextDouble(-1.0, 1.0) }

    private fun inBounds(case: Case, ih: Int, iw: Int) = ih in 0 until case.inH && iw in 0 until case.inW

    private fun referenceForward(case: Case, input: DoubleArray, kernels: DoubleArray, biases: DoubleArray): DoubleArray {
        val out = DoubleArray(case.outShape.size)
        for (f in 0 until case.filters) for (oh in 0 until case.outShape.height) for (ow in 0 until case.outShape.width) {
            var sum = biases[f]
            for (c in 0 until case.inC) for (kh in 0 until case.k) for (kw in 0 until case.k) {
                val ih = oh * case.stride - case.pad + kh
                val iw = ow * case.stride - case.pad + kw
                if (inBounds(case, ih, iw)) {
                    sum += input[case.inShape.index(ih, iw, c)] * kernels[((f * case.inC + c) * case.k + kh) * case.k + kw]
                }
            }
            out[case.outShape.index(oh, ow, f)] = sum
        }
        return out
    }

    @Test
    fun `convolution forward and backward match the definitions across shapes, strides and padding`() {
        val rng = Random(11)
        for (case in cases) {
            val input = rng.sparse(case.inShape.size)
            val kernels = DoubleArray(case.filters * case.inC * case.k * case.k) { rng.nextDouble(-1.0, 1.0) }
            val biases = DoubleArray(case.filters) { rng.nextDouble(-1.0, 1.0) }
            val outGrad = rng.sparse(case.outShape.size)
            val label = "${case.inShape} k${case.k} s${case.stride} p${case.pad} -> ${case.outShape}"

            val out = DoubleArray(case.outShape.size) { 1.0 }
            ConvOps.conv2d(input, case.inShape, kernels, case.filters, case.k, biases, out, case.outShape, case.stride, case.pad, case.pad)
            assertArrayEquals(referenceForward(case, input, kernels, biases).map { it + 1.0 }.toDoubleArray(), out, 1e-12, "forward $label")

            val expectedInputGrad = DoubleArray(case.inShape.size)
            val expectedKernelGrad = DoubleArray(kernels.size)
            val expectedBiasGrad = DoubleArray(case.filters)
            for (f in 0 until case.filters) for (oh in 0 until case.outShape.height) for (ow in 0 until case.outShape.width) {
                val g = outGrad[case.outShape.index(oh, ow, f)]
                expectedBiasGrad[f] += g
                for (c in 0 until case.inC) for (kh in 0 until case.k) for (kw in 0 until case.k) {
                    val ih = oh * case.stride - case.pad + kh
                    val iw = ow * case.stride - case.pad + kw
                    if (!inBounds(case, ih, iw)) continue
                    val ki = ((f * case.inC + c) * case.k + kh) * case.k + kw
                    val ii = case.inShape.index(ih, iw, c)
                    expectedInputGrad[ii] += g * kernels[ki]
                    expectedKernelGrad[ki] += g * input[ii]
                }
            }

            val inputGrad = DoubleArray(case.inShape.size) { 2.0 }
            ConvOps.conv2dBackwardInput(outGrad, case.outShape, kernels, case.filters, case.k, inputGrad, case.inShape, case.stride, case.pad, case.pad)
            assertArrayEquals(expectedInputGrad.map { it + 2.0 }.toDoubleArray(), inputGrad, 1e-12, "input gradient $label")

            val kernelGrad = DoubleArray(kernels.size) { 3.0 }
            val biasGrad = DoubleArray(case.filters) { 4.0 }
            ConvOps.conv2dBackwardKernels(outGrad, case.outShape, input, case.inShape, kernelGrad, case.filters, case.k, biasGrad, case.stride, case.pad, case.pad)
            assertArrayEquals(expectedKernelGrad.map { it + 3.0 }.toDoubleArray(), kernelGrad, 1e-12, "kernel gradient $label")
            assertArrayEquals(expectedBiasGrad.map { it + 4.0 }.toDoubleArray(), biasGrad, 1e-12, "bias gradient $label")
        }
    }
}
