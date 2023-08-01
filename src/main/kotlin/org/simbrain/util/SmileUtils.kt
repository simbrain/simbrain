package org.simbrain.util

import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.plot.histogram.HistogramPanel
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

// TODO: Flatten the two arrays so that this can be used for arbitrary matrices (currently works only on vectors)
infix fun Matrix.sse(other: Matrix) = (this.toDoubleArray() sse other.toDoubleArray())

/**
 * Returns the matrix at a row, transposed.
 * A common requirement because Simbrain generally assumes column vectors.
 * A minor performance improvement, but originates in an effort to work around a bug with the MKL implementation.
 */
fun Matrix.rowVectorTransposed(rowIndex: Int): Matrix {
    if (rowIndex !in 0 until nrow()) {
        throw IllegalArgumentException("Invalid row index $rowIndex")
    }
    val ret = Matrix(ncol(),1)
    for (i in 0 until ncol()) {
        ret[i,0] = get(rowIndex, i)
    }
    return ret
}

/**
 * Convert a double array to a Smile Matrix / column vector.
 */
fun DoubleArray.toMatrix() = Matrix.column(this)!!

/**
 * Add the entries of a double array in-place to a Smile matrix / column vector. Assumes the matrix has as many rows
 * as the array has entries.
 */
fun Matrix.add(toAdd: DoubleArray) {
    if (this.nrow() != toAdd.size) {
        throw IllegalArgumentException("Trying to add a double array of length ${toAdd.size} to a matrix with ${nrow
            ()} rows")
    }
    (0 until nrow()).forEach { i -> set(i,0, toAdd[i]) }
}

operator fun Matrix.plusAssign(toAdd: DoubleArray) = add(toAdd)

operator fun Matrix.plusAssign(toAdd: Matrix) { add(toAdd) }

operator fun Matrix.minusAssign(toSubtract: Matrix) { sub(toSubtract) }

fun Matrix.clip(min: Double, max: Double) {
    for (i in 0 until nrow()) {
        for (j in 0 until ncol()) {
            set(i,j, get(i,j).coerceIn(min, max))
        }
    }
}

operator fun Matrix.get(i: Int) = if (ncol() != 1) throw IllegalStateException("Must be a column vector") else get(i, 0)

fun Matrix.toDoubleArray() = if (ncol() != 1) throw IllegalStateException("Must be a column vector") else col(0)!!

fun Matrix.toSequence(): Sequence<Double> = if (ncol() != 1) throw IllegalStateException("Must be a column vector")
else sequence {
    for (i in (0 until this@Matrix.nrow())) {
        yield(this@Matrix.get(i, 0))
    }
}

/**
 * Returns a new matrix whose entries are shifted to the right by the indicated amount.
 */
fun Matrix.shiftRight(shiftAmount: Int = 1): Matrix {
    val shiftedMatrix = Matrix(nrow(), ncol())
    for (i in 0 until nrow()) {
        for (j in 0 until ncol()) {
            shiftedMatrix[i,j] = this[i, (j + ncol() - shiftAmount) % ncol()]
        }
    }
    return shiftedMatrix
}


/**
 * Element-wise multiplication that broadcasts across columns.
 *
 * Assumes vector is a column vector and that it has as many rows as the matrix.
 */
fun Matrix.broadcastMultiply(vector: Matrix): Matrix {
    if (vector.ncol() != 1) {
        throw IllegalArgumentException("Vector is ${vector.shapeString}, but it must be a column vector")
    }
    if (nrow() != vector.nrow()) {
        throw IllegalArgumentException("Matrix and vector must have the same number of rows. Matrix has ${nrow()} " +
                "rows, vector has ${vector.nrow()} rows .")
    }
    val result = Matrix(nrow(), ncol())
    for (i in 0 until nrow()) {
        for (j in 0 until ncol()) {
            result[i, j] = this[i, j] * vector[i, 0]
        }
    }
    return result
}

fun Matrix.flatten(): DoubleArray = flattenArray(toArray())

/**
 * Display a histogram for the (flattened) matrix.
 * Returns an updater function so that the histogram can be updated when the matrix changes.  That function takes a
 * matrix as an argument in case the matrix to be rendered changes (example: randomizing a weight matrix create a new matrix).
 */
@JvmOverloads
fun Matrix.showHistogram(title: String = "Show Histogram", label: String = ""): (Matrix) -> Unit {
    val histogramPanel = HistogramPanel(HistogramModel())
    histogramPanel.model.resetData(mutableListOf(this.flatten()), mutableListOf(label))
    histogramPanel.displayInDialog().apply { this.title = title }
    return { matrix -> histogramPanel.model.resetData(mutableListOf(matrix.flatten()), mutableListOf(label)) }
}