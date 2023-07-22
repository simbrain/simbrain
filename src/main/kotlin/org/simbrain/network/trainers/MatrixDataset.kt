package org.simbrain.network.trainers

import org.simbrain.util.shiftRight
import smile.math.matrix.Matrix
import kotlin.math.min

class MatrixDataset(val inputs: Matrix, val targets: Matrix) {

    init {
        if (inputs.nrow() != targets.nrow()) {
            throw IllegalArgumentException("inputs and targets must be the same size")
        }
        if (inputs.nrow() == 0) {
            throw IllegalArgumentException("tables should not be empty")
        }
    }

    val size get() = inputs.nrow()

}

/**
 * Creates a dataset where the inputs and targets are both diagonal matrices of appropriate sizes.
 * A shift amount can be provided to shift the target array to the right.
 *
 * Provides a simple default training set
 */
fun createDiagonalDataset(nInputs: Int, nOutputs: Int, shiftAmount: Int = 0): MatrixDataset {
    val nrows = min(nInputs, nOutputs)
    return MatrixDataset(Matrix.eye(nrows, nInputs), Matrix.eye(nrows, nOutputs).shiftRight(shiftAmount))
}