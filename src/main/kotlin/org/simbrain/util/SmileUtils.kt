package org.simbrain.util

import smile.math.matrix.Matrix

fun Matrix.validateShape(target: Matrix) {
    if (target.nrows() != nrows() || target.ncols() != ncols()) {
        throw IllegalArgumentException("Matrix with shape $shapeString does not match matrix with shape $shapeString")
    }
}

val Matrix.shapeString get() = "(${nrows()},${ncols()})"
