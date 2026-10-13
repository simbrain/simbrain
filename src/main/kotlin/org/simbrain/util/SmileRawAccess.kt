/**
 * Direct access to the backing array of a Smile [Matrix], for elementwise loops over many parameters (optimizer
 * steps) that the JIT can vectorize. Smile keeps the array package-private; it is reached through a [VarHandle], and
 * callers fall back to [Matrix.get] and [Matrix.set] when it is unavailable.
 */
package org.simbrain.util

import smile.math.blas.Layout
import smile.math.matrix.Matrix
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle

private val backingArrayHandle: VarHandle? = runCatching {
    MethodHandles.privateLookupIn(Matrix::class.java, MethodHandles.lookup())
        .findVarHandle(Matrix::class.java, "A", DoubleArray::class.java)
}.getOrNull()

/**
 * The column-major backing array of this matrix, or null if the matrix is not column-major or the array cannot be
 * reached. Entry (i, j) is at `j * ld() + i`; columns can be padded, so `ld()` may exceed the row count.
 */
fun Matrix.columnMajorArrayOrNull(): DoubleArray? {
    if (layout() != Layout.COL_MAJOR) return null
    return backingArrayHandle?.get(this) as DoubleArray?
}

/**
 * The backing arrays of [matrices] if all of them are column-major with the same shape and leading dimension, so one
 * index addresses the same entry in each; otherwise null.
 */
fun alignedColumnMajorArrays(vararg matrices: Matrix): List<DoubleArray>? {
    val first = matrices.first()
    if (matrices.any { it.nrow() != first.nrow() || it.ncol() != first.ncol() || it.ld() != first.ld() }) return null
    return matrices.map { it.columnMajorArrayOrNull() ?: return null }
}
