package org.simbrain.network.core

import org.simbrain.util.UserParameter
import org.simbrain.util.randomize
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix

/**
 * Array based layers (based on Smile matrices) should extend this. Maintains an input vector for summing inputs.
 */
abstract class ArrayLayer(
    private val parent: Network,
    inputSize: Int
) : Layer() {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    val increment = .1
        get() = field

    @UserParameter(label = "Clamped", description = "Clamping", order = 3)
    var isClamped = false
        set(clamped) {
            field = clamped
            events.clampChanged.fireAndForget()
        }

    override val inputs: Matrix = Matrix(inputSize, 1)

    override val network: Network
        get() = parent

    override fun inputSize(): Int {
        return inputs.size().toInt()
    }

    override fun updateInputs() {
        val wtdInputs = Matrix(inputSize(), 1)
        for (c in incomingConnectors) {
            wtdInputs.add(c.output)
        }
        addInputs(wtdInputs)
    }

    override fun addInputs(inputs: Matrix) {
        this.inputs.add(inputs)
    }

    @Consumable
    fun addInputs(inputs: DoubleArray?) {
        addInputs(Matrix.column(inputs))
    }

    override fun randomize() {
        // TODO: Find non-obtrusive way to set randomizer for cases besides neuronarray
        inputs.randomize(UniformRealDistribution())
        events.updated.fireAndForget()
    }

    override fun clear() {
        inputs.mul(0.0)
        events.updated.fireAndForget()
    }

    override fun increment() {
        inputs.add(increment)
        events.updated.fireAndForget()
    }

    override fun decrement() {
        inputs.sub(increment)
        events.updated.fireAndForget()
    }

    override fun toggleClamping() {
        isClamped = !isClamped
    }

    @Producible
    fun getInputActivations() = inputs.col(0)
}