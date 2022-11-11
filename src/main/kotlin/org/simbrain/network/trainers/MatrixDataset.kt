package org.simbrain.network.trainers

import smile.math.matrix.Matrix

class MatrixDataset(val inputs: Matrix, val targets: Matrix) {

    constructor(nin: Int, nout: Int) : this(Matrix.eye(nin), Matrix.eye(nout))

}