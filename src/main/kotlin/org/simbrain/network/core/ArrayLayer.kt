package org.simbrain.network.core

import org.simbrain.util.UserParameter
import org.simbrain.util.addi
import org.simbrain.util.reshape
import org.simbrain.util.toMatrix
import org.simbrain.workspace.Consumable
import smile.math.matrix.Matrix

/**
 * Array based layers (based on Smile matrices) should extend this. Maintains an input vector for summing inputs.
 */
abstract class ArrayLayer(
    val inputSize: Int
) : Layer() {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    var increment = .1

    @UserParameter(label = "Clamped", description = "Clamping", order = 3)
    var isClamped = false
        set(clamped) {
            field = clamped
            events.clampChanged.fire()
        }

    override val inputs: Matrix = Matrix(inputSize, 1)

    override val size: Int get() = inputs.size().toInt()

    context(Network)
    override fun accumulateInputs() {
        super.accumulateInputs()
        val wtdInputs = Matrix(size, 1)
        for (c in incomingConnectors) {
            wtdInputs.addi(c.getSummedPSRs())
        }
        addInputs(wtdInputs)
        addInputs(biases)
    }

    @Consumable
    override fun addInputs(inputs: Matrix) {
        this.inputs.add(inputs)
    }

    @Consumable
    fun addInputs(inputs: DoubleArray?) {
        addInputs(Matrix.column(inputs))
    }

    /**
     * Add input array even if size is mismatched, in which case the input is reshaped. See [reshape]
     */
    @Consumable
    fun addInputsMismatched(inputs: DoubleArray) {
        addInputs(inputs.toMatrix().reshape(this.inputs.nrow(), this.inputs.ncol()))
    }

    override fun clear() {
        inputs.mul(0.0)
        events.updated.fire()
    }

    override fun increment() {
        inputs.add(increment)
        events.updated.fire()
    }

    override fun decrement() {
        inputs.sub(increment)
        events.updated.fire()
    }

    override fun toggleClamping() {
        isClamped = !isClamped
    }
}