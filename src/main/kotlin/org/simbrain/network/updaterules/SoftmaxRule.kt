package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.flatten
import org.simbrain.util.setRow
import org.simbrain.util.toDoubleArray
import smile.math.matrix.Matrix
import kotlin.math.exp

class SoftmaxRule: NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), BoundedUpdateRule {

    @UserParameter(
        label = "Temperature",
        description = """Above 1 is a "hotter", more chaotic, and thus flatter distribution. 0 to 1 is a "cooler", more predictable, sharper distribution.""",
        minimumValue = 0.0,
        increment = .1,
        order = 10)
    var temperature = 1.0

    private fun softmax(input: Matrix, temperature: Double, bias: Matrix = Matrix(input.nrow(), 1)): DoubleArray {
        // These are often called "logits", that is, a set of unnormalized values
        val max = input.flatten().max() // for max normalization to avoid overflow
        val exponentials = (input.toDoubleArray() zip bias.toDoubleArray()).map { (i, b) -> exp(((i + b) - max) / temperature) }
        val total = exponentials.sum()
        
        // for small values, return a uniform distribution
        if (total < 1e-6) {
            return DoubleArray(exponentials.size) { 1.0 / exponentials.size }
        }
        return exponentials.map { it/total }.toDoubleArray()
    }

    /**
     * Apply softmax row-wise to a matrix. Each row gets its own softmax distribution.
     * Used for ActivationSequence where each row represents one sequence position.
     */
    private fun softmaxRowWise(input: Matrix, temperature: Double, bias: Matrix): Matrix {
        val result = Matrix(input.nrow(), input.ncol())
        
        for (rowIndex in 0 until input.nrow()) {
            val rowVector = input.row(rowIndex)
            
            // Handle bias: use the first row if bias has only one row (broadcast), 
            // otherwise use the corresponding row
            val biasVector = if (bias.nrow() == 1) {
                // For single row bias, if it's a column vector, broadcast the single value
                if (bias.ncol() == 1) {
                    DoubleArray(input.ncol()) { bias[0, 0] } // Broadcast single bias value
                } else {
                    bias.row(0) // Use the bias row directly
                }
            } else if (rowIndex < bias.nrow()) {
                if (bias.ncol() == 1) {
                    DoubleArray(input.ncol()) { bias[rowIndex, 0] } // Broadcast row-specific bias value
                } else {
                    bias.row(rowIndex) // Use row-specific bias
                }
            } else {
                DoubleArray(input.ncol()) { 0.0 } // Zero bias if no bias available
            }
            
            // Apply softmax to this row
            val max = rowVector.max() // for numerical stability
            val exponentials = rowVector.mapIndexed { colIndex, value -> 
                exp(((value + biasVector[colIndex]) - max) / temperature) 
            }
            val total = exponentials.sum()
            
            // Handle edge case of very small exponentials
            val probabilities = if (total < 1e-6) {
                DoubleArray(exponentials.size) { 1.0 / exponentials.size }
            } else {
                exponentials.map { it / total }.toDoubleArray()
            }
            
            result.setRow(rowIndex, probabilities)
        }
        
        return result
    }

    context(Network) override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        val inputs = layer.inputs
        val biases = layer.biases
        
        if (inputs.nrow() == 1 || inputs.ncol() == 1) {
            // Single vector case: use existing logic (either row or column vector)
            layer.setActivations(softmax(inputs, temperature, biases))
        } else {
            // Multiple rows case (ActivationSequence): apply softmax row-wise
            layer.activations = softmaxRowWise(inputs, temperature, biases)
        }
    }

    context(Network) override fun apply(neuron: Neuron, data: EmptyScalarData) {
        throw UnsupportedOperationException("SoftmaxRule does not support scalar data")
    }

    override val name = "Softmax"
    override val timeType = Network.TimeType.DISCRETE

    override fun createMatrixData(size: Int): EmptyMatrixData {
        return EmptyMatrixData
    }

    override fun copy() = SoftmaxRule().also {
        it.temperature = temperature
    }

    override var upperBound: Double
        get() = 1.0
        set(value) {}

    override var lowerBound: Double
        get() = 0.0
        set(value) {}
}