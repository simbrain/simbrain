package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import smile.math.matrix.Matrix
import kotlin.math.ln

class SmileUtilsTest {

    val testMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0)
    ))

    val nonSquareMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0),
        doubleArrayOf(10.0, 11.0, 12.0)
    ))

    @Test
    fun `test validate shape`() {
        val a = Matrix(1, 2)
        val b = Matrix(2, 1)
        assertDoesNotThrow{ a.validateSameShape(a) }
        assertThrows<IllegalArgumentException> { a.validateSameShape(b) }

        // Uncomment to check the exception formatting
        // a.validateShape(b)
    }

    @Test
    fun `test row matrix transposed`() {
        val rmt = nonSquareMatrix.rowVectorTransposed(0)
        assertEquals(3, rmt.nrow())
        assertEquals(1, rmt.ncol())
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), rmt.toDoubleArray())
    }

    @Test
    fun `test broadcasting multiplication`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toMatrix()
        val result = testMatrix.broadcastMultiply(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), result.col(0))
        assertArrayEquals(doubleArrayOf(2.0, 5.0, 8.0), result.col(1))
        assertArrayEquals(doubleArrayOf(6.0, 12.0, 18.0), result.col(2))
    }

    @Test
    fun `test broadcasting multiplication on non square matrix`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toMatrix()
        val result = nonSquareMatrix.broadcastMultiply(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0, 0.0), result.col(0))
        assertArrayEquals(doubleArrayOf(2.0, 5.0, 8.0, 11.0), result.col(1))
        assertArrayEquals(doubleArrayOf(6.0, 12.0, 18.0, 24.0), result.col(2))
    }

    @Test
    fun testMaxEigen() {
        assertEquals(1.0, Matrix.eye(2).maxEigenvalue())
        assertEquals(2.0, Matrix.eye(2).mul(2.0).maxEigenvalue())
        val ut=  Matrix.of(arrayOf(
            doubleArrayOf(-2.0, 4.0),
            doubleArrayOf(0.0, 7.0)
        ))
        assertEquals(7.0, ut.maxEigenvalue())
    }

    @Test
    fun testSpectralRadius() {
        assertEquals(.9, testMatrix.setSpectralRadius(.9).maxEigenvalue(), .01)
    }

    @Test
    fun shiftUpAndPadEndWithZero() {
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), nonSquareMatrix.shiftUpAndPadEndWithZero().row(0))
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), nonSquareMatrix.shiftUpAndPadEndWithZero().row(nonSquareMatrix.nrow() - 1))
    }

    @Test
    fun `test add operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0)
        ).toMatrix()
        val c = a + b
        assertArrayEquals(doubleArrayOf(6.0, 8.0), c.row(0))
        assertArrayEquals(doubleArrayOf(10.0, 12.0), c.row(1))
    }

    @Test
    fun `test minus operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0)
        ).toMatrix()
        val c = a - b
        assertArrayEquals(doubleArrayOf(-4.0, -4.0), c.row(0))
        assertArrayEquals(doubleArrayOf(-4.0, -4.0), c.row(1))
    }

    @Test
    fun `test scalar multiplication operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = a * 2.0
        assertArrayEquals(doubleArrayOf(2.0, 4.0), b.row(0))
        assertArrayEquals(doubleArrayOf(6.0, 8.0), b.row(1))
        val c = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val d = -2.0 * c
        assertArrayEquals(doubleArrayOf(-2.0, -4.0), d.row(0))
        assertArrayEquals(doubleArrayOf(-6.0, -8.0), d.row(1))
    }

    @Test
    fun `test cross entropy loss`() {
        val t1 =  doubleArrayOf(0.0, 1.0, 0.0).toMatrix()
        val a1 =  doubleArrayOf(0.2, .7, 0.1).toMatrix()
        assertEquals(0.0, crossEntropy(t1, t1), 0.001)
        assertEquals(-ln(.7), crossEntropy(a1, t1))
    }


}
