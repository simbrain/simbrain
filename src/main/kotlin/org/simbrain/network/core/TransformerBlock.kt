package org.simbrain.network.core

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import smile.stat.distribution.GaussianDistribution
import java.awt.geom.Rectangle2D
import kotlin.math.exp
import kotlin.math.sqrt

class TransformerBlock(val stackSize: Int, inputSize: Int, val hiddenSize: Int): ArrayLayer(inputSize), EditableObject {

    override val inputs: Matrix = Matrix(stackSize, inputSize)

    @UserParameter(label = "Activations", description = "Activations in the stack", order = 1)
    override var activations: Matrix = Matrix(stackSize, inputSize)

    override val activationArray: DoubleArray
        get() = activations.flatten()

    val K = Matrix(inputSize, inputSize)
    val Q = Matrix(inputSize, inputSize)
    val V = Matrix(inputSize, inputSize)

    val kStack = Matrix(stackSize, inputSize)
    val qStack = Matrix(stackSize, inputSize)
    val vStack = Matrix(stackSize, inputSize)

    val selfAttention = Matrix(stackSize, stackSize)


    // Feedforward network parameters
    val W1 = Matrix(inputSize, hiddenSize)
    val b1 = Matrix(stackSize, hiddenSize)
    val W2 = Matrix(hiddenSize, inputSize)
    val b2 = Matrix(stackSize, inputSize)

    val feedForwardInput = Matrix(stackSize, inputSize)

    val feedForwardHidden = Matrix(stackSize, hiddenSize)

    override val biases: Matrix get() = throw UnsupportedOperationException("Not applicable to Transformer")

    override val biasArray: DoubleArray
        get() = throw UnsupportedOperationException("Not applicable to Transformer")


    override val size: Int = inputSize

    override val bound: Rectangle2D = Rectangle2D.Double()

    context(Network) override fun accumulateInputs() {
        val matrix = (incomingConnectors.firstOrNull() as? WeightMatrix)?.weightMatrix
        (incomingConnectors.firstOrNull()?.source as? ActivationStack)?.let { source ->
            inputs.add(source.activations.mm(matrix?.transpose()))
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        Q.copyFrom(Matrix.rand(
            inputSize, inputSize,
            GaussianDistribution(0.0, 1.0)
        ))
        K.copyFrom(Matrix.rand(
            inputSize, inputSize,
            GaussianDistribution(0.0, 1.0)
        ))
        V.copyFrom(Matrix.rand(
            inputSize, inputSize,
            GaussianDistribution(0.0, 1.0)
        ))
        W1.copyFrom(Matrix.rand(
            inputSize, hiddenSize,
            GaussianDistribution(0.0, 1.0)
        ))
        W2.copyFrom(Matrix.rand(
            hiddenSize, inputSize,
            GaussianDistribution(0.0, 1.0)
        ))
        events.updated.fire()
    }

    private fun softmaxRow(row: DoubleArray): DoubleArray {
        val max = row.maxOrNull() ?: 0.0  // For numerical stability
        val expValues = row.map { exp(it - max) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toDoubleArray()
    }


    context(Network) override fun update() {
        if (isClamped) {
            return
        }

        val scale = sqrt(inputSize.toDouble())

        kStack.copyFrom(inputs.mm(K))
        qStack.copyFrom(inputs.mm(Q))
        vStack.copyFrom(inputs.mm(V))

        selfAttention.copyFrom(qStack.mm(kStack.transpose()))
        selfAttention.div(scale)

        // Apply softmax to each row
        (0 until selfAttention.nrow())
            .map { selfAttention.row(it) }
            .map { row ->
                softmaxRow(row)
            }.toTypedArray()
            .toMatrix()
            .let { selfAttention.copyFrom(it) }

        val attentionOutput = selfAttention.mm(vStack)

        feedForwardInput.copyFrom(inputs.clone().add(attentionOutput).layerNorm())

        feedForwardHidden.copyFrom(feedForwardInput.mm(W1).add(b1).relu())

        activations.copyFrom(feedForwardInput.add(feedForwardHidden.mm(W2).add(b2)).layerNorm())

        inputs.mul(0.0)
        events.updated.fire()
    }


    class CreationTemplate : EditableObject {

        @UserParameter(label = "Stack Size", description = "Number of activation vectors in the stack", order = 1)
        var stackSize = 7

        @UserParameter(label = "Input Size", description = "Number of inputs to each activation", order = 2)
        var inputSize = 4

        @UserParameter(label = "Hidden Size", description = "Size of the hidden layer in the feedforward network", order = 3)
        var hiddenSize = 16

        fun create(): TransformerBlock {
            return TransformerBlock(stackSize, inputSize, hiddenSize)
        }

        override val name = "Activation Stack"

    }
}

fun main() {
    val source = ActivationStack(7, 4)
    val target = ActivationStack(7, 6)

    source.activations[0, 0] = 1.0
    source.activations[1, 1] = 1.0
    source.activations[2, 2] = 1.0
    source.activations[3, 3] = 1.0

    val wm = WeightMatrix(source, target)

    wm.randomize()

    val net = Network()
    net.addNetworkModels(source, target, wm)

    println(target.activations)

    net.update()

    println(target.activations)


}
