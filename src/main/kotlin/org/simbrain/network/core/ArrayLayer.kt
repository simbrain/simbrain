package org.simbrain.network.core

import org.simbrain.util.UserParameter
import org.simbrain.util.randomize
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.Consumable
import smile.math.matrix.Matrix

/**
 * Array based layers (based on Smile matrices) should extend this. Maintains an input vector for summing inputs.
 */
abstract class ArrayLayer(
    /**
     * Reference to network this array is part of.
     */
    private val parent: Network, inputSize: Int
) : Layer() {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    val increment = .1
        get() = field

    @UserParameter(label = "Clamped", description = "Clamping", order = 3)
    var isClamped = false
        set(clamped) {
            field = clamped
            events.fireClampChanged()
        }

    /**
     * Collects inputs from other network models using arrays.
     */
    val inputs: Matrix

    init {
        inputs = Matrix(inputSize, 1)
    }

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

    override fun addInputs(newInputs: Matrix) {
        inputs.add(newInputs)
    }

    @Consumable
    fun addInputs(inputs: DoubleArray?) {
        addInputs(Matrix(inputs))
    }

    override fun randomize() {
        // TODO: Find non-obtrusive way to set randomizer for cases besides neuronarray
        inputs.randomize(UniformRealDistribution())
        events.fireUpdated()
    }

    override fun clear() {
        inputs.mul(0.0)
        events.fireUpdated();
    }

    override fun increment() {
        inputs.add(increment)
        events.fireUpdated()
    }

    override fun decrement() {
        inputs.sub(increment)
        events.fireUpdated()
    }

    override fun getNetwork(): Network {
        return parent
    }

    override fun toggleClamping() {
        isClamped = !isClamped
    }
}