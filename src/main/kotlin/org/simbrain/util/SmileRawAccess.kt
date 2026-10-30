/**
 * Direct access to the backing array of a Smile [Matrix], for elementwise loops over many parameters (optimizer
 * steps) and matrix-vector products that the JIT can vectorize. Smile keeps the array package-private; it is reached through a [VarHandle], and
 * callers fall back to [Matrix.get] and [Matrix.set] when it is unavailable.
 */
package org.simbrain.util

import smile.math.blas.Layout
import smile.math.blas.Transpose
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

/** Writes [values] into column [j]. */
fun Matrix.setColumn(j: Int, values: DoubleArray) {
    val columns = columnMajorArrayOrNull()
    if (columns != null) values.copyInto(columns, j * ld(), 0, nrow()) else for (i in 0 until nrow()) this[i, j] = values[i]
}

/** The sum of the squares of all entries. */
fun Matrix.sumOfSquares(): Double {
    val values = columnMajorArrayOrNull()
    var sum = 0.0
    for (j in 0 until ncol()) {
        if (values != null) {
            val start = j * ld()
            for (k in start until start + nrow()) sum += values[k] * values[k]
        } else {
            for (i in 0 until nrow()) sum += this[i, j] * this[i, j]
        }
    }
    return sum
}

/** A copy of this matrix in the default column-major layout, which a new matrix of the same shape also has. */
fun Matrix.columnMajorCopy(): Matrix = Matrix(nrow(), ncol()).also { it.copyFrom(this) }

/**
 * Writes this matrix times [x] into [out]. On the column-major array it walks eight columns per pass, loading and
 * storing each output entry once per eight columns. That beat OpenBLAS's matrix-vector product at every size tried,
 * from 10x100 to 4000x4000, on an Apple M1 Pro and an AMD Ryzen AI Max+ 395, single-threaded or not: its threads only
 * add hand-off cost to products this cheap. Other layouts go through BLAS. The sum order differs from BLAS, so results
 * can differ in the last bits.
 */
fun Matrix.timesVectorInto(x: DoubleArray, out: DoubleArray) {
    val rows = nrow()
    val cols = ncol()
    require(x.size >= cols && out.size >= rows) { "Vector sizes ${x.size} and ${out.size} do not fit a ${rows}x$cols matrix" }
    val a = columnMajorArrayOrNull()
    if (a == null) {
        mv(Transpose.NO_TRANSPOSE, 1.0, x, 0.0, out)
        return
    }
    val ld = ld()
    out.fill(0.0, 0, rows)
    var j = 0
    while (j + 7 < cols) {
        val x0 = x[j]; val x1 = x[j + 1]; val x2 = x[j + 2]; val x3 = x[j + 3]
        val x4 = x[j + 4]; val x5 = x[j + 5]; val x6 = x[j + 6]; val x7 = x[j + 7]
        val s0 = j * ld; val s1 = s0 + ld; val s2 = s1 + ld; val s3 = s2 + ld
        val s4 = s3 + ld; val s5 = s4 + ld; val s6 = s5 + ld; val s7 = s6 + ld
        for (r in 0 until rows) {
            out[r] += a[s0 + r] * x0 + a[s1 + r] * x1 + a[s2 + r] * x2 + a[s3 + r] * x3 +
                    a[s4 + r] * x4 + a[s5 + r] * x5 + a[s6 + r] * x6 + a[s7 + r] * x7
        }
        j += 8
    }
    while (j < cols) {
        val xj = x[j]
        val s = j * ld
        for (r in 0 until rows) out[r] += a[s + r] * xj
        j++
    }
}

/**
 * Adds this matrix times [x] into [out], visiting only the columns where [x] is nonzero, for a sparse [x] such as the
 * sources that spiked in one update. Column by column, so on the column-major array each visited column is one
 * contiguous run.
 */
fun Matrix.timesSparseVectorInto(x: DoubleArray, out: DoubleArray) {
    val columns = columnMajorArrayOrNull()
    val ld = ld()
    for (i in x.indices) {
        val factor = x[i]
        if (factor == 0.0) continue
        if (columns != null) {
            val start = i * ld
            for (j in out.indices) out[j] += columns[start + j] * factor
        } else {
            for (j in out.indices) out[j] += this[j, i] * factor
        }
    }
}
