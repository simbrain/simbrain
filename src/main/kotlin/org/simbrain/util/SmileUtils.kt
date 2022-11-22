package org.simbrain.util

import smile.math.matrix.Matrix

/**
 * Make sure the two matrices have the same shape
 */
fun Matrix.validateSameShape(target: Matrix) {
    if (target.nrows() != nrows() || target.ncols() != ncols()) {
        throw IllegalArgumentException("Matrix with shape $shapeString does not match matrix with shape $shapeString")
    }
}

val Matrix.shapeString get() = "(${nrows()},${ncols()})"

/**
 * Returns the matrix at a row, transposed.
 * A minor performance improvement, but originates in an effort to work around a bug with the MKL implementation.
 */
fun Matrix.rowMatrixTransposed(rowIndex: Int): Matrix {
    if (rowIndex !in 0..nrows()) {
        throw IllegalArgumentException("Invalid row index $rowIndex")
    }
    val ret = Matrix(ncols(),1)
    for (i in 0 until ncols()) {
        ret[i,0] = get(rowIndex, i)
    }
    return ret
}

fun Matrix.rowMatrix(rowIndex: Int) = row(*intArrayOf(rowIndex))

fun Matrix.colMatrix(rowIndex: Int) = col(*intArrayOf(rowIndex))
