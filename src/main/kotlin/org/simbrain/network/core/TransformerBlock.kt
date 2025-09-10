package org.simbrain.network.core

import org.simbrain.network.gui.dialogs.NetworkPreferences.biasesRandomizer
import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.network.trainers.StructuredProbe
import org.simbrain.network.trainers.WeightInitializationStrategy
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import kotlin.math.exp
import kotlin.math.pow
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
class TransformerBlock(val sequenceSize: Int, inputSize: Int, val hiddenSize: Int) : ArrayLayer(inputSize),
    EditableObject, ActivationSequenceProcessor {

    /**
     * Size of inputs is same as outputs in a transformer block, and also going into the feedforward part
     * That is, the "size" of the "residual stream"
     */
    override val size: Int = inputSize

    override val inputs: Matrix = Matrix(sequenceSize, size)

    /**
     * Activations here are the output layer of the feed forward network. Activations by convention in Simbrain are
     * the "output" of a layer, which for example weight matrices read from.
     */
    @UserParameter(label = "Activations", description = "Output activations", order = 1)
    override var activations: Matrix = Matrix(sequenceSize, size)

    @UserParameter(label = "Matrix visibility", description = "Show the QKV matrices.", order = 10)
    var matrixVisibility = true
        set(value) {
            field = value
            events.updateGraphics.fire()
        }

    @UserParameter(label = "Sequence visibility", description = "Show the qkv sequences.", order = 11)
    var sequenceVisibility = true
        set(value) {
            field = value
            events.updateGraphics.fire()
        }

    @UserParameter(label = "Feedforward visibility", description = "Show the feedforward network.", order = 12)
    var feedForwardVisibility = true
        set(value) {
            field = value
            events.updateGraphics.fire()
        }

    @UserParameter(label = "Layer norm", description = "Use layer normalization.", order = 13)
    var useLayerNorm = true

    @UserParameter(label = "User positional encoding", description = "Use user positional encoding", order = 14)
    var userPositionalEncoding = false

    @UserParameter(label = "Triangular mask", description = "Use triangular mask.", order = 15)
    var useTriangularMask = true

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

    override var biases: Matrix
        get() = throw UnsupportedOperationException("Not applicable to Transformer")
        set(value) {
            throw UnsupportedOperationException("Not applicable to Transformer")
        }

    override val biasArray: DoubleArray
        get() = throw UnsupportedOperationException("Not applicable to Transformer")

    override val updateRule: NeuronUpdateRule<*, *>
        get() = throw UnsupportedOperationException("Not applicable to Transformer")

    context(Network) override fun accumulateInputs() {
        val matrix = (incomingConnectors.firstOrNull() as? WeightMatrix)?.weights
        (incomingConnectors.firstOrNull()?.source as? ActivationSequenceProcessor)?.let { source ->
            inputs.add(source.activations.mm(matrix?.transpose()))
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        listOf(K, Q, V, W1, W2).forEach { it.randomize((randomizer ?: weightRandomizer)) }
        listOf(b1, b2).forEach { it.randomize(biasesRandomizer) }
        events.updated.fire()
    }

    fun initWeights(weightInitializationStrategy: WeightInitializationStrategy) {
        listOf(K, Q, V, W1, W2).forEach {
            weightInitializationStrategy.initializeWeights(it)
        }
    }

    fun initBiases() {
        listOf(b1, b2).forEach { it.randomize(biasesRandomizer) }
    }

    override fun clear() {
        listOf(
            activations, kStack, qStack, vStack, selfAttention, attentionOutput,
            feedForwardInput, feedForwardHiddenNetInputs, feedForwardHidden, feedForwardOutputNetInputs
        ).forEach { it.fill(0.0) }
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

        if (userPositionalEncoding) {
            val positionalEncoding = Matrix(sequenceSize, size).apply {
                for (i in 0 until sequenceSize) {
                    for (j in 0 until size) {
                        this[i, j] = kotlin.math.sin(i.toDouble() / (10000.0.pow(j.toDouble() / size)))
                    }
                }
            }
            inputs.add(positionalEncoding)
        }

        val scale = sqrt(size.toDouble())

        kStack.copyFrom(inputs.mm(K))
        qStack.copyFrom(inputs.mm(Q))
        vStack.copyFrom(inputs.mm(V))

        selfAttention.copyFrom(qStack.mm(kStack.transpose()))
        selfAttention.div(scale)

        // Mask out the upper triangle of the self-attention matrix
        if (useTriangularMask) {
            selfAttention.setValuesInPlace { i, j ->
                if (i < j) Double.NEGATIVE_INFINITY else selfAttention[i, j]
            }
        }

        // Apply softmax to each row
        (0 until selfAttention.nrow())
            .map { selfAttention.row(it) }
            .map { row ->
                softmaxRow(row)
            }.toTypedArray()
            .toMatrix()
            .let { selfAttention.copyFrom(it) }

        attentionOutput.copyFrom(selfAttention.mm(vStack))

        // Skip connection from main inputs to MLP input
        feedForwardInput.copyFrom(
            inputs.clone().add(attentionOutput).let { if (useLayerNorm) it.layerNormByRow() else it })

        // MLP computation
        feedForwardHiddenNetInputs.copyFrom(feedForwardInput.mm(W1.transpose()).addToEachRow(b1))
        feedForwardHidden.copyFrom(feedForwardHiddenNetInputs.relu())
        feedForwardOutputNetInputs.copyFrom(feedForwardHidden.mm(W2.transpose()).addToEachRow(b2))

        // Skip connection from MLP input to output of transformer (activations)
        activations.copyFrom(
            feedForwardInput.clone().add(feedForwardOutputNetInputs)
                .let { if (useLayerNorm) it.layerNormByRow() else it })

        inputs.fill(0.0)
        events.updated.fire()
    }

    override fun processError(
        error: Matrix,
        signalSource: Layer,
        biasesAccumulator: HashMap<Layer, Matrix>,
        rawMatrixAccumulator: HashMap<Matrix, Matrix>,
        probe: StructuredProbe?
    ): Matrix {

        var errorSignal = error

        // Handle different error signal formats based on source type
        errorSignal = when (signalSource) {
            is NeuronArray -> {
                // For NeuronArray source, we currently only get error for the last position
                // But we should extend this to support multiple targets for proper autoregressive training
                // For now, maintain current behavior but prepare for future enhancement
                feedForwardOutputNetInputs.clone().apply {
                    fill(0.0)
                    setRow(nrow() - 1, errorSignal.toDoubleArray())
                }
            }

            is ActivationSequenceProcessor -> {
                // Sequence-to-sequence: error signal is already a matrix with errors for each position
                error
            }

            else -> {
                // Fallback: use the error signal as-is
                error
            }
        }

        rawMatrixAccumulator.getOrPut(b2) {
            Matrix(b2.nrow(), b2.ncol())
        }.add(errorSignal.colSums().toColumnVector())

        val dFinalOutput = errorSignal

        // Split into feedForwardInput (for the second skip connection) and feedForwardOutput
        val dFeedForwardInput = dFinalOutput.clone()
        dFinalOutput.clone()

        // Weight deltas layer 2
        val W2Delta = errorSignal.transpose().mm(feedForwardHidden)
        rawMatrixAccumulator.getOrPut(W2) {
            Matrix(W2.nrow(), W2.ncol())
        }.add(W2Delta)

        // Bias deltas hidden layer
        errorSignal = errorSignal.mm(W2)
        errorSignal = errorSignal.mul(feedForwardHiddenNetInputs.reluDerivative())
        rawMatrixAccumulator.getOrPut(b1) {
            Matrix(b1.nrow(), b1.ncol())
        }.add(errorSignal.colSums().toColumnVector())

        // Weight deltas layer 1
        val W1Delta = errorSignal.transpose().mm(feedForwardInput)
        rawMatrixAccumulator.getOrPut(W1) {
            Matrix(W1.nrow(), W1.ncol())
        }.add(W1Delta)

        errorSignal = errorSignal.mm(W1)

        val dFeedforwardInputFromMLP = errorSignal.clone()
        // Incorporate second skip connection and store for use with first skip connection
        val dFeedforwardInputTotal = dFeedforwardInputFromMLP.clone().add(dFeedForwardInput)

        // Then pass that total to the attention block:
        val dAttentionOutput = dFeedforwardInputTotal.clone()

        // delta V
        val dVStack = selfAttention.transpose().mm(dAttentionOutput)
        val dV = inputs.transpose().mm(dVStack)
        rawMatrixAccumulator.getOrPut(V) { Matrix(V.nrow(), V.ncol()) }.add(dV)

        // Backprop through selfAttention = softmax(QK^T / sqrt(d))
        val dSelfAttention = dFeedforwardInputFromMLP.mm(vStack.transpose())

        // Mask out the upper triangle of the self-attention matrix with zeros
        dSelfAttention.setValuesInPlace { i, j ->
            if (i < j) 0.0 else dSelfAttention[i, j]
        }

        val dQK = softmaxBackward(dSelfAttention, selfAttention).div(sqrt(size.toDouble()))

        // Delta Q is inputs "outer product" derivative of the QStack
        val dQStack = dQK.mm(kStack)
        val dQ = inputs.transpose().mm(dQStack)
        rawMatrixAccumulator.getOrPut(Q) { Matrix(Q.nrow(), Q.ncol()) }.add(dQ)

        // Delta K is inputs "outer product" derivative of the KStacks
        val dKStack = dQK.transpose().mm(qStack)
        val dK = inputs.transpose().mm(dKStack)
        rawMatrixAccumulator.getOrPut(K) { Matrix(K.nrow(), K.ncol()) }.add(dK)

        // Backprop all gradients wrt inputs and return their sum
        val dInputsFromK = dKStack.mm(K.transpose())
        val dInputsFromQ = dQStack.mm(Q.transpose())
        val dInputsFromV = dVStack.mm(V.transpose())
        val dInputsTotal = dFeedforwardInputTotal
            .add(dInputsFromK)
            .add(dInputsFromQ)
            .add(dInputsFromV)
        return dInputsTotal
    }

    override fun copy() = TransformerBlock(sequenceSize, inputSize, hiddenSize).also {
        it.activations.copyFrom(activations)
        it.matrixVisibility = matrixVisibility
        it.sequenceVisibility = sequenceVisibility
        it.feedForwardVisibility = feedForwardVisibility
        it.useLayerNorm = useLayerNorm
        it.userPositionalEncoding = userPositionalEncoding
        it.useTriangularMask = useTriangularMask
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
        it.attentionOutput.copyFrom(attentionOutput)
        it.feedForwardInput.copyFrom(feedForwardInput)
        it.feedForwardHiddenNetInputs.copyFrom(feedForwardHiddenNetInputs)
        it.feedForwardHidden.copyFrom(feedForwardHidden)
        it.feedForwardOutputNetInputs.copyFrom(feedForwardOutputNetInputs)
    }

    class CreationTemplate : EditableObject {

        @UserParameter(label = "Stack size", description = "Number of activation vectors in the sequence", order = 1)
        var sequenceSize = 7

        @UserParameter(label = "Input size", description = "Number of inputs to the layer", order = 2)
        var inputSize = 4

        @UserParameter(
            label = "Hidden size",
            description = "Size of the hidden layer in the feedforward network",
            order = 3
        )
        var hiddenSize = 16

        fun create(): TransformerBlock {
            return TransformerBlock(sequenceSize, inputSize, hiddenSize)
        }

        override val name = "Transformer Block"
    }


    override fun toString(): String {
        return """
                Name: $displayName ($sequenceSize x $inputSize)
                Sequence Size: $sequenceSize 
                Input Size: $inputSize 
                Hidden Layer Size: $hiddenSize
            """.trimIndent()
        //Output Size (activations) = ${activations.shapeString}
        //K=${K.shapeString}, Q=${Q.shapeString}, V=${V.shapeString}
    }
}
