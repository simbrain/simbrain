/**
 * The raw-array matrix-vector product matches the naive product, on and off its eight-column stride and for matrices
 * without a column-major array.
 */
package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix
import kotlin.random.Random

class SmileRawAccessTest {

    private fun naiveProduct(m: Matrix, x: DoubleArray) = DoubleArray(m.nrow()) { i ->
        (0 until m.ncol()).sumOf { j -> m[i, j] * x[j] }
    }

    @Test
    fun `matrix times vector matches the naive product for column counts on and off the eight-column stride`() {
        val rng = Random(3)
        for ((rows, cols) in listOf(1 to 1, 3 to 7, 10 to 8, 17 to 9, 150 to 400, 64 to 15)) {
            val m = Matrix(rows, cols).apply { for (i in 0 until rows) for (j in 0 until cols) this[i, j] = rng.nextDouble() - 0.5 }
            val x = DoubleArray(cols) { rng.nextDouble() - 0.5 }
            val out = DoubleArray(rows) { 99.0 }
            m.timesVectorInto(x, out)
            assertArrayEquals(naiveProduct(m, x), out, 1e-12, "${rows}x$cols")
        }
    }

    @Test
    fun `matrix times vector works on a matrix without a column-major array`() {
        val rng = Random(4)
        val m = Matrix(6, 11).apply { for (i in 0 until 6) for (j in 0 until 11) this[i, j] = rng.nextDouble() }.transpose()
        val x = DoubleArray(6) { rng.nextDouble() }
        val out = DoubleArray(11)
        m.timesVectorInto(x, out)
        assertArrayEquals(naiveProduct(m, x), out, 1e-12)
    }
}
