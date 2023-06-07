package org.simbrain.network.trainers

import smile.math.matrix.Matrix

class MatrixDataset(val inputs: Matrix, val targets: Matrix) {

    // TODO: Validate same rows for inputs and targets on primary constructor

    constructor(nInputs: Int, nOutputs: Int, nrows: Int = 10)
            : this(Matrix.eye(nrows+1, nInputs), Matrix.eye(nrows+1, nOutputs))

}