package org.simbrain.network.core

import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * A simplified Transformer block with no attention heads, inspired by the architecture introduced in Vaswani et al. (2017).
 *
 * The current design allows [ActivationSequence] to be connected to a transformer block with a weight matrix
 * and a [NeuronArray] assumed to be softmax connected on the output side. Only the last vector in the output
 * activation sequence is sent to the neuron array.
 *
 * Multiple transformer blocks connected together is not yet supported but should not be hard.
 *
 * @see <a href="https://arxiv.org/abs/1706.03762">Attention Is All You Need</a>
 */
class TransformerBlock(val sequenceSize: Int, inputSize: Int, val hiddenSize: Int): ArrayLayer(inputSize), EditableObject, ActivationSequenceProcessor {

    /**
     * Size of inputs is same as outputs in a transformer block
     */
    override val size: Int = inputSize

    override val inputs: Matrix = Matrix(sequenceSize, size)

    /**
     * Activations here are the output layer of the feed forward network. Activations by convention in Simbrain are
     * the "output" of a layer, which for example weight matrices read from.
     */
    @UserParameter(label = "Activations", description = "Output activations", order = 1)
    override var activations: Matrix = Matrix(sequenceSize, size)

    @UserParameter(label = "Matrix Visibility", description = "Show the QKV matrices", order = 10)
    var matrixVisibility = true
        set(value) {
            field = value
            events.updateGraphics.fire()
        }

    @UserParameter(label = "Sequence Visibility", description = "Show the qkv sequences", order = 11)
    var sequenceVisibility = true
        set(value) {
            field = value
            events.updateGraphics.fire()
        }

    @UserParameter(label = "Feedforward Visibility", description = "Show the feedforward network", order = 12)
    var feedForwardVisibility = true
        set(value) {
            field = value
            events.updateGraphics.fire()
        }

    /**
     * Output activations as double array
     */
    override val activationArray: DoubleArray
        get() = activations.flatten()

    val K = Matrix(size, size)
    val Q = Matrix(size, size)
    val V = Matrix(size, size)

    val kStack = Matrix(sequenceSize, size)
    val qStack = Matrix(sequenceSize, size)
    val vStack = Matrix(sequenceSize, size)

    val selfAttention = Matrix(sequenceSize, sequenceSize)

    val attentionOutput = Matrix(sequenceSize, size)

    // Feedforward network parameters
    val W1 = Matrix(hiddenSize, size) // up projection
    val b1 = Matrix(hiddenSize, 1)
    val W2 = Matrix(size, hiddenSize) // down projection
    val b2 = Matrix(size, 1)

    val feedForwardInput = Matrix(sequenceSize, size)

    val feedForwardHiddenNetInputs = Matrix(sequenceSize, hiddenSize)
    val feedForwardHidden = Matrix(sequenceSize, hiddenSize)

    val feedForwardOutputNetInputs = Matrix(sequenceSize, size)

    override val biases: Matrix get() = throw UnsupportedOperationException("Not applicable to Transformer")

    override val biasArray: DoubleArray
        get() = throw UnsupportedOperationException("Not applicable to Transformer")

    context(Network) override fun accumulateInputs() {
        val matrix = (incomingConnectors.firstOrNull() as? WeightMatrix)?.weightMatrix
        (incomingConnectors.firstOrNull()?.source as? ActivationSequenceProcessor)?.let { source ->
            inputs.add(source.activations.mm(matrix?.transpose()))
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        fun Matrix.applyRandomizer() {
            randomize((randomizer ?: weightRandomizer))
        }
        listOf(K, Q, V, W1, b1, W2, b2).forEach { it.applyRandomizer() }
        events.updated.fire()
    }

    private fun softmaxRow(row: DoubleArray): DoubleArray {
        val max = row.maxOrNull() ?: 0.0  // For numerical stability
        val expValues = row.map { exp(it - max) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toDoubleArray()
    }

    private fun softmaxBackward(dOut: Matrix, out: Matrix): Matrix {
        val dIn = Matrix(dOut.nrow(), dOut.ncol())
        for (i in 0 until out.nrow()) {
            val y = out.row(i)
            val dy = dOut.row(i)
            // For each row: dIn = (dy - sum(dy * y)) * y
            val dot = dy.dot(y)
            val rowResult = DoubleArray(y.size) { j ->
                (dy[j] - dot) * y[j]
            }
            dIn.setRow(i, rowResult)
        }
        return dIn
    }

    context(Network) override fun update() {
        if (isClamped) {
            return
        }

        val scale = sqrt(size.toDouble())

        kStack.copyFrom(inputs.mm(K))
        qStack.copyFrom(inputs.mm(Q))
        vStack.copyFrom(inputs.mm(V))

        selfAttention.copyFrom(qStack.mm(kStack.transpose()))
        // TODO: Add masking using triangular matrix
        selfAttention.div(scale)

        // Apply softmax to each row
        (0 until selfAttention.nrow())
            .map { selfAttention.row(it) }
            .map { row ->
                softmaxRow(row)
            }.toTypedArray()
            .toMatrix()
            .let { selfAttention.copyFrom(it) }

        attentionOutput.copyFrom(selfAttention.mm(vStack))

        feedForwardInput.copyFrom(inputs.clone().add(attentionOutput).layerNormByRow())

        feedForwardHiddenNetInputs.copyFrom(feedForwardInput.mm(W1.transpose()).addToEachRow(b1))
        feedForwardHidden.copyFrom(feedForwardHiddenNetInputs.relu())

        feedForwardOutputNetInputs.copyFrom(feedForwardHidden.mm(W2.transpose()).addToEachRow(b2))
        activations.copyFrom(feedForwardInput.add(feedForwardOutputNetInputs).layerNormByRow())

        inputs.mul(0.0)
        events.updated.fire()
    }

    override fun accumulateBackprop(gradient: Matrix, rawMatrixAccumulator: HashMap<Matrix, Matrix>): Matrix {

        var layerError = gradient

        layerError = feedForwardOutputNetInputs.reluDerivative().broadcastMultiply(layerError)
        rawMatrixAccumulator.getOrPut(b2) {
            Matrix(b2.nrow(), b2.ncol())
        }.add(layerError.colSums().toMatrix())

        val W2Delta = layerError.transpose().mm(feedForwardHidden)

        rawMatrixAccumulator.getOrPut(W2) {
            Matrix(W2.nrow(), W2.ncol())
        }.add(W2Delta)

        layerError = layerError.mm(W2)

        layerError = layerError.mul(feedForwardHiddenNetInputs.reluDerivative())
        rawMatrixAccumulator.getOrPut(b1) {
            Matrix(b1.nrow(), b1.ncol())
        }.add(layerError.colSums().toMatrix())

        val W1Delta = layerError.transpose().mm(feedForwardInput)
        rawMatrixAccumulator.getOrPut(W1) {
            Matrix(W1.nrow(), W1.ncol())
        }.add(W1Delta)

        layerError = layerError.mm(W1)

        // feedForwardInput = inputs + attentionOutput
        // So gradient splits evenly:
        val dFeedForwardInput = layerError
        val dAttentionOutput = layerError.clone() // Since attentionOutput is just added to inputs, it receives the same gradient
        val dInputs_fromSum = layerError.clone()  // The portion of gradient w.r.t inputs from the sum

        // Now backprop through attentionOutput = selfAttention.mm(vStack)
        val dSelfAttention = dAttentionOutput.mm(vStack.transpose())
        val dVStack = selfAttention.transpose().mm(dAttentionOutput)

        // Accumulate gradient w.r.t. V: vStack = inputs.mm(V)
        val dV = inputs.transpose().mm(dVStack)
        rawMatrixAccumulator.getOrPut(V) { Matrix(V.nrow(), V.ncol()) }.add(dV)
        val dInputs_fromV = dVStack.mm(V.transpose())

        // Backprop through selfAttention = softmax(QK^T / sqrt(d))
        // We'll need a softmax backward pass function: softmaxBackward(dOut, out) -> dIn
        val dQK = softmaxBackward(dSelfAttention, selfAttention).div(sqrt(size.toDouble()))

        // QK^T = qStack.mm(kStack.transpose())
        val dQStack = dQK.mm(kStack)
        val dKStack = dQK.transpose().mm(qStack)

        // qStack = inputs.mm(Q)
        val dQ = inputs.transpose().mm(dQStack)
        rawMatrixAccumulator.getOrPut(Q) { Matrix(Q.nrow(), Q.ncol()) }.add(dQ)
        val dInputs_fromQ = dQStack.mm(Q.transpose())

        // kStack = inputs.mm(K)
        val dK = inputs.transpose().mm(dKStack)
        rawMatrixAccumulator.getOrPut(K) { Matrix(K.nrow(), K.ncol()) }.add(dK)
        val dInputs_fromK = dKStack.mm(K.transpose())

        // Combine all gradients w.r.t. inputs
        val dInputs_total = dInputs_fromSum.add(dInputs_fromQ).add(dInputs_fromK).add(dInputs_fromV)

        // Return the gradient w.r.t. inputs, which can be passed further back in the network
        return dInputs_total
    }

    fun copy() = TransformerBlock(sequenceSize, inputSize, hiddenSize).also {
        it.activations.copyFrom(activations)
        it.K.copyFrom(K)
        it.Q.copyFrom(Q)
        it.V.copyFrom(V)
        it.W1.copyFrom(W1)
        it.b1.copyFrom(b1)
        it.W2.copyFrom(W2)
        it.b2.copyFrom(b2)
        it.kStack.copyFrom(kStack)
        it.qStack.copyFrom(qStack)
        it.vStack.copyFrom(vStack)
        it.selfAttention.copyFrom(selfAttention)
        it.feedForwardInput.copyFrom(feedForwardInput)
        it.feedForwardHidden.copyFrom(feedForwardHidden)
    }

    class CreationTemplate : EditableObject {

        @UserParameter(label = "Stack Size", description = "Number of activation vectors in the sequence", order = 1)
        var sequenceSize = 7

        @UserParameter(label = "Input Size", description = "Number of inputs to the layer", order = 2)
        var inputSize = 4

        @UserParameter(label = "Hidden Size", description = "Size of the hidden layer in the feedforward network", order = 3)
        var hiddenSize = 16

        fun create(): TransformerBlock {
            return TransformerBlock(sequenceSize, inputSize, hiddenSize)
        }

        override val name = "Transformer Block"

    }
}
