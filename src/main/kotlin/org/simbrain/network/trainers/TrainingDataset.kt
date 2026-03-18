package org.simbrain.network.trainers

import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix

class TrainingDataset(
    val inputs: MutableList<MutableList<Double>>,
    val targets: MutableList<MutableList<Double>>,
    val inputSize: Int = if (inputs.isNotEmpty()) inputs[0].size else throw IllegalArgumentException("Cannot infer input size from empty data. Use constructor with explicit sizes."),
    val targetSize: Int = if (targets.isNotEmpty()) targets[0].size else throw IllegalArgumentException("Cannot infer target size from empty data. Use constructor with explicit sizes."),
    val inputRowNames: List<String>? = null,
    val targetRowNames: List<String>? = null,
    val inputColumnNames: List<String>? = null,
    val targetColumnNames: List<String>? = null
): Iterable<Pair<List<Double>, List<Double>>> {

    init {
        if (inputs.size != targets.size) {
            throw IllegalArgumentException("inputs and targets must be the same size")
        }
        if (inputs.isNotEmpty()) {
            inputs.forEach { input ->
                if (input.size != inputSize) {
                    throw IllegalArgumentException("Input row has ${input.size} columns, expected $inputSize")
                }
            }
        }
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                if (target.size != targetSize) {
                    throw IllegalArgumentException("Target row has ${target.size} columns, expected $targetSize")
                }
            }
        }
    }

    val size get() = inputs.size

    override fun iterator(): Iterator<Pair<List<Double>, List<Double>>> = object : Iterator<Pair<List<Double>, List<Double>>> {
        private var index = 0
        override fun hasNext() = index < size
        override fun next(): Pair<List<Double>, List<Double>> {
            if (!hasNext()) throw NoSuchElementException()
            return (inputs[index] to targets[index]).also { index++ }
        }
    }

    fun getInputRow(index: Int): Matrix = inputs[index].toDoubleArray().toColumnVector()
    
    fun getTargetRow(index: Int): Matrix = targets[index].toDoubleArray().toColumnVector()

}
