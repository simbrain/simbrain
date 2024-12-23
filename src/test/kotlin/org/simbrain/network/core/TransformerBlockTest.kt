package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.toMatrix
import smile.math.matrix.Matrix

class TransformerBlockTest {

    var net = Network()

    private val sequenceSize = 2
    private val inputSize = 2
    private val hiddenSize = 2

    // Create a block with small dimensions so manual math is tractable
    private val block = TransformerBlock(sequenceSize, inputSize, hiddenSize)

    private val EPSILON = 1e-6
    private val TOL = 1e-3

    init {
        // Manually set all block weights to known values, so the test is deterministic.
        // K, Q, V each 2x2
        block.K[0, 0] = 0.1; block.K[0, 1] = 0.2
        block.K[1, 0] = 0.3; block.K[1, 1] = 0.4

        block.Q[0, 0] = -0.1; block.Q[0, 1] = 0.2
        block.Q[1, 0] =  0.3; block.Q[1, 1] = -0.4

        block.V[0, 0] = 0.0; block.V[0, 1] = 0.1
        block.V[1, 0] = 0.2; block.V[1, 1] = 0.3

        // W1, W2 each 2x2 in this example
        block.W1[0, 0] = 1.0; block.W1[0, 1] = 0.0
        block.W1[1, 0] = 0.0; block.W1[1, 1] = 1.0

        block.W2[0, 0] = 1.0; block.W2[0, 1] = 0.0
        block.W2[1, 0] = 0.0; block.W2[1, 1] = 1.0

        // Biases (2x1)
        block.b1[0, 0] = 0.0; block.b1[1, 0] = 0.0
        block.b2[0, 0] = 0.0; block.b2[1, 0] = 0.0
    }

    @Test
    fun `forward pass numeric test`() {
        // 1) Construct input using toMatrix(). The shape is (2 rows, 2 columns).
        val inputMatrix = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()

        // 2) Supply the input to the block
        block.addInputs(inputMatrix)

        // 3) Perform a forward pass
        with(net) { block.update() }

        // 4) Now we compare the block’s internal states to manually computed references.

        // -- (A) Check kStack, qStack, vStack:

        //  kStack = inputs.mm(K)
        //    For row0=(1,2),  K=[[0.1,0.2],[0.3,0.4]]:
        //      => row0 = (1*0.1 + 2*0.3, 1*0.2 + 2*0.4) = (0.7, 1.0)
        //    For row1=(3,4):
        //      => row1 = (3*0.1 + 4*0.3, 3*0.2 + 4*0.4) = (1.5, 2.2)
        val kStackExpected = arrayOf(
            doubleArrayOf(0.7, 1.0),
            doubleArrayOf(1.5, 2.2)
        ).toMatrix()
        checkMatrixEquals(kStackExpected, block.kStack, "kStack")

        //  qStack = inputs.mm(Q)
        //    Q=[[ -0.1, 0.2 ], [0.3, -0.4]]
        //    row0 => (1*(-0.1) + 2*0.3, 1*0.2 + 2*(-0.4)) = (0.5, -0.6)
        //    row1 => (3*(-0.1) + 4*0.3, 3*0.2 + 4*(-0.4)) = (0.9, -1.0)
        val qStackExpected = arrayOf(
            doubleArrayOf(0.5, -0.6),
            doubleArrayOf(0.9, -1.0)
        ).toMatrix()
        checkMatrixEquals(qStackExpected, block.qStack, "qStack")

        //  vStack = inputs.mm(V)
        //    V=[[0.0,0.1],[0.2,0.3]]
        //    row0 => (1*0.0 + 2*0.2, 1*0.1 + 2*0.3) = (0.4, 0.7)
        //    row1 => (3*0.0 + 4*0.2, 3*0.1 + 4*0.3) = (0.8, 1.5)
        val vStackExpected = arrayOf(
            doubleArrayOf(0.4, 0.7),
            doubleArrayOf(0.8, 1.5)
        ).toMatrix()
        checkMatrixEquals(vStackExpected, block.vStack, "vStack")

        // -- (B) Check selfAttention:
        //   selfAttention = softmax( (qStack.mm(kStack.transpose())) / sqrt(d) ) row-wise.
        //   d = inputSize=2 => sqrt(2)=1.4142

        //   qStack.mm(kStack^T) => shape (2x2)
        //   row0 dot row0 => 0.5*0.7 + (-0.6)*1.0 = 0.35 - 0.6 = -0.25
        //   row0 dot row1 => 0.5*1.5 + (-0.6)*2.2 = 0.75 - 1.32 = -0.57
        //   row1 dot row0 => 0.9*0.7 + (-1.0)*1.0 = 0.63 - 1.0 = -0.37
        //   row1 dot row1 => 0.9*1.5 + (-1.0)*2.2 = 1.35 - 2.2 = -0.85

        //   => qkT = [[-0.25, -0.57],
        //             [-0.37, -0.85]]
        //   => qkT / sqrt(2) ~ [[-0.1768, -0.4033],
        //                       [-0.2618, -0.6010]]

        //   row0 exponentials => e^-0.1768 ~ 0.838, e^-0.4033 ~ 0.668 => sum ~ 1.506 => softmax => ~ [0.556, 0.444]
        //   row1 exponentials => e^-0.2618 ~ 0.769, e^-0.6010 ~ 0.548 => sum ~ 1.317 => softmax => ~ [0.584, 0.416]
        val selfAttentionExpected = arrayOf(
            doubleArrayOf(0.556, 0.444),
            doubleArrayOf(0.584, 0.416)
        ).toMatrix()
        checkMatrixEquals(selfAttentionExpected, block.selfAttention, "selfAttention", tol = 1e-3)

        // check softmax sums to 1
        assertArrayEquals(doubleArrayOf(1.0, 1.0), block.selfAttention.rowSums())


        // -- (C) attentionOutput = selfAttention.mm(vStack)

        //   row0 => (0.556*0.4 + 0.444*0.8, 0.556*0.7 + 0.444*1.5)
        //         => (0.2224 + 0.3552, 0.3892 + 0.666) = (0.5776, 1.0552)
        //   row1 => (0.584*0.4 + 0.416*0.8, 0.584*0.7 + 0.416*1.5)
        //         => (0.2336 + 0.3328, 0.4088 + 0.624) = (0.5664, 1.0328)
        val attentionOutExpected = arrayOf(
            doubleArrayOf(0.5776, 1.0552),
            doubleArrayOf(0.5664, 1.0328)
        ).toMatrix()
        checkMatrixEquals(attentionOutExpected, block.attentionOutput, "attentionOutput", tol = 1e-3)

        // -- (D) The final block.activations includes skip connections + layer norms + feedforward net.
        //     We'll just confirm it’s not empty and shape is correct for now.
        assertEquals(sequenceSize, block.activations.nrow(), "activations row count mismatch")
        assertEquals(inputSize, block.activations.ncol(), "activations col count mismatch")

        // -- (E) Check the feedforward net output

        checkMatrixEquals(attentionOutExpected.clone().add(inputMatrix), block.feedForwardHidden, "feedForwardHidden", tol = 1e-3)

    }

    /**
     * Helper function to compare two matrices element by element within a tolerance.
     */
    private fun checkMatrixEquals(
        expected: Matrix,
        actual: Matrix,
        name: String,
        tol: Double = EPSILON
    ) {
        assertEquals(expected.nrow(), actual.nrow(), "$name row count mismatch")
        assertEquals(expected.ncol(), actual.ncol(), "$name col count mismatch")
        for (i in 0 until expected.nrow()) {
            for (j in 0 until expected.ncol()) {
                assertEquals(
                    expected[i, j],
                    actual[i, j],
                    tol,
                    "Mismatch at $name($i,$j)"
                )
            }
        }
    }

}