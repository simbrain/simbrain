package org.simbrain.util

/**
 * Consider sets A and B.  "Left complement" is in A but not B.  "Right complement" is in B but not A.
 */
data class SetDifference<T>(val leftComp: Set<T>, val rightComp: Set<T>) {
    fun isIdentical() = leftComp.isEmpty() && rightComp.isEmpty()
}

/**
 * Custom infix relative complementation operator.
 */
infix fun <T> Set<T>.complement(other: Set<T>) = SetDifference(this - other, other - this)

/**
 * Map a pair of lists to a list of pairs.
 *
 * Ex: (1,2) cartesianProduct (3,4) -> ((1,3),(1,4),(2,3), (2,4))
 */
infix fun <T,U> Iterable<T>.cartesianProduct(other: Iterable<U>) = this.flatMap { a -> other.map { b -> a to b }}

/**
 * [cartesianProduct] for a pair of sequences.
 */
infix fun <T, U> Sequence<T>.cartesianProduct(other: Sequence<U>)
    = this.flatMap { a -> other.map { b -> a to b }}

/**
 * Flatten a 2d double array into a 1-d double array
 */
fun flattenArray(array : Array<DoubleArray>) = sequence {
    for (row in array) {
        for (element in row) {
            yield(element)
        }
    }
}.toList().toDoubleArray()

/**
 * Reshape a 1-d double array into a 2d array with the indicated number of rows and columns.
 */
fun reshape(rows: Int, cols: Int, array: DoubleArray) =
    Array(rows) { i ->
        val row = DoubleArray(cols)
        for (j in 0 until cols) {
            row[j] = array[i * cols + j]
        }
        row
    }