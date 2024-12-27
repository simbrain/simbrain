package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.plus
import org.simbrain.util.relu
import org.simbrain.util.toMatrix
import smile.math.matrix.Matrix

class TransformerBlockTest {

    var net = Network()

    private val sequenceSize = 2
    private val inputSize = 2
    private val hiddenSize = 2

    // Create a block with small dimensions so manual math is tractable
    private val block = TransformerBlock(sequenceSize, inputSize, hiddenSize).apply {
        useLayerNorm = false // Assuming no layer norm for now
    }

    private val EPSILON = 1e-6

    init {
        // Manually set all block weights to known values
        block.K[0, 0] = 1.0; block.K[0, 1] = 0.0
        block.K[1, 0] = 0.0; block.K[1, 1] = 1.0

        block.Q[0, 0] = 1.0; block.Q[0, 1] = 0.0
        block.Q[1, 0] = 0.0; block.Q[1, 1] = 1.0

        block.V[0, 0] = 1.0; block.V[0, 1] = 0.0
        block.V[1, 0] = 0.0; block.V[1, 1] = 1.0

        // W1, W2 each 2x2 in this example
        block.W1[0, 0] = 1.0; block.W1[0, 1] = 0.0
        block.W1[1, 0] = 0.0; block.W1[1, 1] = 1.0

        block.W2[0, 0] = 1.0; block.W2[0, 1] = 0.0
        block.W2[1, 0] = 0.0; block.W2[1, 1] = 1.0

        // Biases
        block.b1[0, 0] = 0.0; block.b1[1, 0] = 0.0
        block.b2[0, 0] = 0.0; block.b2[1, 0] = 0.0
    }

    @Test
    fun `forward pass numeric test`() {

        // Set up inputs
        val inputMatrix = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        block.addInputs(inputMatrix)

        // Forward pass
        with(net) { block.update() }

        // Check K, Q, V multiplications
        val kStackExpected = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        checkMatrixEquals(kStackExpected, block.kStack, "kStack")

        val qStackExpected = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        checkMatrixEquals(qStackExpected, block.qStack, "qStack")

        val vStackExpected = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        checkMatrixEquals(vStackExpected, block.vStack, "vStack")

        // Check selfAttention:
        //   selfAttention = softmax( (qStack.mm(kStack.transpose())) / sqrt(scale) ) row-wise.
        //   scale = sqrt(inputSize) = sqrt(2) = 1.4142
        //   Computed in python
        //   Row 1 = [0.01416604, 0.98583396]
        //   Row 2 = [0.0000502, 0.9999498]

        val selfAttentionExpected = arrayOf(
            doubleArrayOf(0.01416604, 0.98583396),
            doubleArrayOf(0.0000502, 0.9999498)
        ).toMatrix()
        checkMatrixEquals(selfAttentionExpected, block.selfAttention, "selfAttention", tol = 1e-3)

        // check softmax rows of self attention matrix sum to 1
        assertArrayEquals(doubleArrayOf(1.0, 1.0), block.selfAttention.rowSums())

        // Attention output
        val attentionOutExpected = selfAttentionExpected.mm(vStackExpected)
        checkMatrixEquals(attentionOutExpected, block.attentionOutput, "attentionOutput", tol = 1e-3)

        // Feed forward inputs: inputs + attentionOutput (skip connection)
        checkMatrixEquals(inputMatrix + attentionOutExpected, block.feedForwardInput, "feedForwardInput", tol = 1e-3)

        // Feed forward hidden net inputs: should be same as feedforward inputs since weights are identity
        checkMatrixEquals(inputMatrix + attentionOutExpected, block.feedForwardHiddenNetInputs, "feedForwardHiddenNetInputs", tol = 1e-3)

        // Feed forward hidden: relu feedForwardHiddenNetInputs
        checkMatrixEquals((inputMatrix + attentionOutExpected).relu(), block.feedForwardHidden, "feedForwardHidden", tol = 1e-3)

        // Feed forward output net inputs: same as feed forward hidden because weights are identity
        checkMatrixEquals((inputMatrix + attentionOutExpected).relu(), block.feedForwardOutputNetInputs, "feedForwardOutputNetInputs", tol = 1e-3)

        // Feed forward output: feedforward input + feed forward output net inputs (another skip connection)
        // This just is the "activation" of the block
        checkMatrixEquals(block.feedForwardInput + block.feedForwardOutputNetInputs, block.activations, "output / activations", tol = 1e-3)

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