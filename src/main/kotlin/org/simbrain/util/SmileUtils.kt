package org.simbrain.util

import smile.math.matrix.Matrix

/**
 * Make sure the two matrices have the same shape
 */
fun Matrix.validateSameShape(target: Matrix) {
    if (target.nrow() != nrow() || target.ncol() != ncol()) {
        throw IllegalArgumentException("Matrix with shape $shapeString does not match matrix with shape " +
                "${target.shapeString}")
    }
}

val Matrix.shapeString get() = "(${nrow()},${ncol()})"

/**
 * Returns the matrix at a row, transposed.
 * A minor performance improvement, but originates in an effort to work around a bug with the MKL implementation.
 */
fun Matrix.rowMatrixTransposed(rowIndex: Int): Matrix {
    if (rowIndex !in 0..nrow()) {
        throw IllegalArgumentException("Invalid row index $rowIndex")
    }
    val ret = Matrix(ncol(),1)
    for (i in 0 until ncol()) {
        ret[i,0] = get(rowIndex, i)
    }
    return ret
}
