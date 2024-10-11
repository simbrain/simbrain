package org.simbrain.network.core

import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.util.UserParameter
import org.simbrain.util.copyFrom
import org.simbrain.util.flatten
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toDoubleArray
import smile.math.matrix.Matrix
import smile.stat.distribution.GaussianDistribution

class ActivationActivationSequence(val sequenceSize: Int, inputSize: Int): ArrayLayer(inputSize), EditableObject, ActivationSequenceProcessor {

    override val inputs: Matrix = Matrix(sequenceSize, inputSize)

    @UserParameter(label = "Activations", description = "Activations in the sequence", order = 1)
    override var activations: Matrix = Matrix(sequenceSize, inputSize)

    override val activationArray: DoubleArray
        get() = activations.flatten()

    override val biases: Matrix = Matrix(inputSize, 1)

    override val biasArray: DoubleArray
        get() = biases.toDoubleArray()


    override val size: Int = inputSize

    context(Network) override fun accumulateInputs() {
        val matrix = (incomingConnectors.firstOrNull() as? WeightMatrix)?.weightMatrix
        (incomingConnectors.firstOrNull()?.source as? ActivationSequenceProcessor)?.let { source ->
            inputs.add(source.activations.mm(matrix?.transpose()))
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        activations.copyFrom(Matrix.rand(
            sequenceSize, inputSize,
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

    fun copy() = ActivationActivationSequence(sequenceSize, inputSize).also {
        it.activations.copyFrom(activations)
    }


    class CreationTemplate : EditableObject {

        @UserParameter(label = "Sequence Size", description = "Number of activation vectors in the sequence", order = 1)
        var sequenceSize = 7

        @UserParameter(label = "Input Size", description = "Length of each activation vector", order = 2)
        var inputSize = 4

        fun create(): ActivationActivationSequence {
            return ActivationActivationSequence(sequenceSize, inputSize)
        }

        override val name = "Activation Sequence"

    }
}

fun main() {
    val source = ActivationActivationSequence(7, 4)
    val target = ActivationActivationSequence(7, 6)

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
