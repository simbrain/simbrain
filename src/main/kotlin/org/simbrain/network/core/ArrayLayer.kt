package org.simbrain.network.core

import org.simbrain.util.UserParameter
import org.simbrain.util.addi
import org.simbrain.util.reshape
import org.simbrain.util.toColumnVector
import org.simbrain.workspace.Consumable
import smile.math.matrix.Matrix

/**
 * Array based layers (based on Smile matrices) should extend this. Maintains an input vector for summing inputs.
 */
abstract class ArrayLayer(
    val inputSize: Int
) : Layer() {

    @UserParameter(
        label = "Increment Amount",
        description = "Amount to increment components when pressing up and down arrows",
        increment = .1,
        order = 20)
    var increment = .1

    @UserParameter(label = "Clamped", description = "Clamping", order = 3)
    override var isClamped = false
        set(clamped) {
            field = clamped
            events.clampChanged.fire()
        }

    abstract override var activations: Matrix

    override val inputs: Matrix = Matrix(inputSize, 1)

    override val size: Int get() = inputs.size().toInt()

    context(Network)
    override fun accumulateInputs() {
        super.accumulateInputs()
        val wtdInputs = Matrix(size, 1)
        for (c in incomingConnectors) {
            val summedPSRs = c.getSummedPSRs()
            wtdInputs.addi(summedPSRs)
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
        addInputs(inputs.toColumnVector().reshape(this.inputs.nrow(), this.inputs.ncol()))
    }

    override fun clearInputs() {
        this.inputs.fill(0.0)
    }

    override fun clear() {
        inputs.fill(0.0)
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