package org.simbrain.network.trainers

import smile.math.matrix.Matrix

class MatrixDataset(val inputs: Matrix, val targets: Matrix) {

    // TODO: Validate same rows for inputs and targets on primary constructor

    constructor(nInputs: Int, nOutputs: Int, nrows: Int = 10)
            : this(Matrix.eye(nrows+1, nInputs), Matrix.eye(nrows+1, nOutputs))

    init {
        if (inputs.nrow() != targets.nrow()) {
            throw IllegalArgumentException("inputs and targets must be the same siz")
        }
        if (inputs.nrow() == 0) {
            throw IllegalArgumentException("tables should not be empty")
        }
    }

    val size get() = inputs.nrow()

}