package org.simbrain.network.core

import org.simbrain.util.UserParameter
import org.simbrain.util.copyFrom
import org.simbrain.util.flatten
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toDoubleArray
import smile.math.matrix.Matrix
import smile.stat.distribution.GaussianDistribution
import java.awt.geom.Rectangle2D

class ActivationStack(val stackSize: Int, inputSize: Int): ArrayLayer(inputSize), EditableObject {

    override val inputs: Matrix = Matrix(stackSize, inputSize)

    @UserParameter(label = "Activations", description = "Activations in the stack", order = 1)
    override var activations: Matrix = Matrix(stackSize, inputSize)

    override val activationArray: DoubleArray
        get() = activations.flatten()

    override val biases: Matrix = Matrix(inputSize, 1)

    override val biasArray: DoubleArray
        get() = biases.toDoubleArray()


    override val size: Int = inputSize

    override val bound: Rectangle2D = Rectangle2D.Double()

    context(Network) override fun accumulateInputs() {
        val matrix = (incomingConnectors.firstOrNull() as? WeightMatrix)?.weightMatrix
        (incomingConnectors.firstOrNull()?.source as? ActivationStack)?.let { source ->
            inputs.add(source.activations.mm(matrix?.transpose()))
        }
        (incomingConnectors.firstOrNull()?.source as? TransformerBlock)?.let { source ->
            inputs.add(source.activations.mm(matrix?.transpose()))
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        activations.copyFrom(Matrix.rand(
            stackSize, inputSize,
            GaussianDistribution(0.0, 1.0)
        ))
        events.updated.fire()
    }

    context(Network) override fun update() {
        if (isClamped) {
            return
        }
        activations.copyFrom(inputs)
        inputs.mul(0.0)
        events.updated.fire()
    }


    class CreationTemplate : EditableObject {

        @UserParameter(label = "Stack Size", description = "Number of activation vectors in the stack", order = 1)
        var stackSize = 7

        @UserParameter(label = "Input Size", description = "Number of inputs to each activation", order = 2)
        var inputSize = 4

        fun create(): ActivationStack {
            return ActivationStack(stackSize, inputSize)
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
