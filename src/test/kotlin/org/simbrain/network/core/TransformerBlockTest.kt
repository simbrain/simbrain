package org.simbrain.network.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.docviewer.DocViewerComponent
import org.simbrain.network.NetworkComponent
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.*
import smile.math.matrix.Matrix
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

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

        // Biases. Setting to 0 for now to simplify testing
        block.b1[0, 0] = 0.0; block.b1[1, 0] = 0.0
        block.b2[0, 0] = 0.0; block.b2[1, 0] = 0.0

        block.label = "TestBlock"

        net.addNetworkModel(block)
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

    @Test
    fun `test backprop numeric checks`() {

        // Forward pass with known input
        val inputMatrix = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()

        block.addInputs(inputMatrix)
        with(net) { block.update() }

        // Test error
        val error = doubleArrayOf(-1.0, 1.0).toMatrix()

        // Dummy source layer and accumulators
        val dummySource = NeuronArray(2)
        val biasesAccumulator = HashMap<ArrayLayer, Matrix>()
        val rawMatrixAccumulator = HashMap<Matrix, Matrix>()

        block.processError(
            error = error,
            signalSource = dummySource,
            biasesAccumulator = biasesAccumulator,
            rawMatrixAccumulator = rawMatrixAccumulator
        )

        // Output bias delta should be the same as the error signal
        val expectedB2delta = error
        checkMatrixEquals(expectedB2delta, rawMatrixAccumulator[block.b2]!!, "b2delta")

        // Error changes shape. Add error as last row in matrix of zeros. TODO
        var errorSignal = Matrix(2,2).apply {
            setRow(1, error.toDoubleArray())
        }

        //print(errorSignal.transpose())
        //print(block.feedForwardHidden)

        // W2 deltas should be error "outer product" hidden activations
        val expectedW2delta = errorSignal.transpose().mm(block.feedForwardHidden)
        //print(expectedW2delta)
        checkMatrixEquals(expectedW2delta, rawMatrixAccumulator[block.W2]!!, "w2delta")

        // Error should be backpropped through weights.
        // Weights are identity so signal unchanged.
        errorSignal = errorSignal.mm(block.W2)
        // Hidden bias deltas are  of hadamard with relu deriv, then colsums
        val expectedB1delta = errorSignal.mul(block.feedForwardHidden.reluDerivative()).colSums().toMatrix()
        checkMatrixEquals(expectedB1delta, rawMatrixAccumulator[block.b1]!!, "b1delta")
        //println(expectedB1delta)

        // W1 deltas
        val expectedW1delta = errorSignal.transpose().mm(block.feedForwardInput)
        //println(errorSignal.transpose())
        //println(block.attentionOutput)
        //print(expectedW1delta)
        checkMatrixEquals(expectedW1delta, rawMatrixAccumulator[block.W1]!!, "w1delta")

        print(rawMatrixAccumulator[block.K])
        print(rawMatrixAccumulator[block.Q])
        print(rawMatrixAccumulator[block.V])

        // TODO: Test the output error signal too, dInputs_total
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

    @Test
    fun testXStream() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        //println(fromXml)
        val openedBlock = fromXml.getModelByLabel(TransformerBlock::class.java, "TestBlock")
        Assertions.assertNotNull(openedBlock)
        assertEquals(2, openedBlock.inputSize)
        assertEquals(2, openedBlock.sequenceSize)
        assertEquals(2, openedBlock.activations.nrow())
        assertEquals(2, openedBlock.activations.ncol())
    }


}