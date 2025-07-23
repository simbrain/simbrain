package org.simbrain.network.core

import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.network.trainers.StructuredProbe
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import smile.stat.distribution.GaussianDistribution

class ActivationSequence(val sequenceSize: Int, inputSize: Int): ArrayLayer(inputSize), EditableObject, ActivationSequenceProcessor {

    override var updateRule: NeuronUpdateRule<ScalarDataHolder, MatrixDataHolder> by GuiEditable(
        initValue = LinearRule(),
        order = 100,
        typeMapProvider = NeuronUpdateRule<*, *>::getNeuronArrayTypeMap,
        setter = {
            val typeChanged = field::class != it::class
            field = it
            if (typeChanged) {
                dataHolder = it.createMatrixData(size)
            }
            events.updated.fire()
        }
    )


    /**
     * Holds data for prototype rule.
     */
    var dataHolder: MatrixDataHolder by GuiEditable(
        label = "State variables",
        initValue = updateRule.createMatrixData(inputSize),
        order = 99,
        onUpdate = {
            val proposedDataHolder = widgetValue(::updateRule).createMatrixData(size)
            if (widgetValue(::dataHolder)::class != proposedDataHolder::class) {
                refreshValue(proposedDataHolder)
            }
        }
    )

    override val inputs: Matrix = Matrix(sequenceSize, inputSize)

    @UserParameter(
        label = "Activations",
        description = "Activations in the sequence",
        order = 1)
    override var activations: Matrix = Matrix(sequenceSize, inputSize)
        set(value) {
            field.copyFrom(value)
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

        val processErrorProbe = probe?.createMapProbe("processError")

        errorSignal = (updateRule as? DifferentiableUpdateRule)?.getDerivative(inputs)?.let { deriv ->
            processErrorProbe?.write("deriv", deriv)
            if (signalSource is NeuronArray) {
                deriv.scaleColumns(errorSignal)
            } else {
                errorSignal.mul(deriv)
            }
        } ?: errorSignal

        processErrorProbe?.write("error") { errorSignal.clone() }

        return errorSignal
    }

    override val activationArray: DoubleArray
        get() = activations.flatten()

    override var biases: Matrix = Matrix(inputSize, 1)

    override val biasArray: DoubleArray
        get() = biases.toDoubleArray()


    override val size: Int = inputSize

    context(Network) override fun accumulateInputs() {
        val matrix = (incomingConnectors.firstOrNull() as? WeightMatrix)?.weights
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

    override fun clear() {
        activations.fill(0.0)
        events.updated.fire()
    }

    context(Network) override fun update() {
        if (isClamped) {
            return
        }
        updateRule.apply(this, dataHolder)
        inputs.fill(0.0)
        events.updated.fire()
    }

    /**
     * Reshapes flattened activations into a stack of input vectors.
     */
    override fun setActivations(activations: DoubleArray) {
        this.activations.copyFrom(activations)
        events.updated.fire()
    }

    override fun copy() = ActivationSequence(sequenceSize, inputSize).also {
        it.activations.copyFrom(activations)
    }

    override val name: String
        get() = displayName

    class CreationTemplate : EditableObject {

        @UserParameter(label = "Sequence Size", description = "Number of activation vectors in the sequence", order = 1)
        var sequenceSize = 7

        @UserParameter(label = "Input Size", description = "Length of each activation vector", order = 2)
        var inputSize = 4

        fun create(): ActivationSequence {
            return ActivationSequence(sequenceSize, inputSize)
        }

        override val name = "Activation Sequence"

    }
}

fun main() {
    val source = ActivationSequence(7, 4)
    val target = ActivationSequence(7, 6)

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
